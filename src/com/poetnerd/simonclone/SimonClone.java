package com.poetnerd.simonclone;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public final class SimonClone {

	public static final int TOTAL_BUTTONS = 3 * 3;
	
	public interface Listener {
		void buttonStateChanged(int index);

		void multipleButtonStateChanged();
	}
	
	private static final String TAG = "Simon Clone Class";
	
	private static final boolean DISABLE_TIMEOUT = false;
		
	/* Classes of messages to handle through our Handler. */
	
	private static final int UI = 0;
	private static final int TIMEOUT = 1;
	
	/* Game States for controlling action of update. */
	
	private static final int IDLE = 0;
	private static final int LISTENING = 1;
	private static final int PLAYING = 2;
	private static final int REPLAYING = 3;
	private static final int LONG_PLAYING = 4;
	private static final int WINNING = 5;
	private static final int WON = 6;
	private static final int LOSING = 7;
	private static final int LOST = 8;
	
	/* Names for the sounds we make */
	
	private static final int GREEN = 0;
	private static final int RED = 1;
	private static final int YELLOW = 2;
	private static final int BLUE = 3;
	private static final int VICTORY_SOUND = 4;
	private static final int LOSE_SOUND = 5;
	private static final int SPECIAL_RAZZ = 6;
	
	private boolean[] activeColors = new boolean [4];
	private int[] longestSequence = new int[32];
	private int[] currentSequence = new int[32];
	
	private int longestLength;
	private int sequenceLength;
	private int sequenceIndex;
	private int totalLength;
	private int playerPosition;
	private long beepDuration;
	private long mLastUpdate;
	private int gameMode;
	private int winToneIndex;
	private int theGame;
	
	private static final Random RNG = new Random();
	private boolean isLit;
	private boolean heardButtonPress;  // Avoid a race of: down -> listen -> up.
	private long pauseDuration;
	
	private boolean[] buttonPressMap = new boolean[TOTAL_BUTTONS];
	
	private List<Listener> listeners = new ArrayList<Listener>();
	
	private SoundPool soundPool;
	
	private int[] soundIds = new int[TOTAL_BUTTONS + 3];  // Add win, lose and razz sounds.
	private int speakerStream;
		
	public SimonClone(Context context) {
		for (int i = 0; i < TOTAL_BUTTONS; ++i) {
			buttonPressMap[i] = false;
		}
		
		soundPool = new SoundPool(TOTAL_BUTTONS, AudioManager.STREAM_MUSIC, 0);
		soundIds[GREEN] = soundPool.load(context, R.raw.green_long, 1);
		soundIds[RED] = soundPool.load(context, R.raw.red_long, 1);
		soundIds[YELLOW] = soundPool.load(context, R.raw.yellow_long, 1);
		soundIds[BLUE] = soundPool.load(context, R.raw.blue_long, 1);
		soundIds[VICTORY_SOUND] = soundPool.load(context, R.raw.victory, 1);
		soundIds[LOSE_SOUND] = soundPool.load(context, R.raw.lose, 1);
		soundIds[SPECIAL_RAZZ] = soundPool.load(context, R.raw.special_razz, 1);

		longestLength = 0;
		sequenceLength = 0;
		sequenceIndex = 0;
		totalLength = 8;  // Level 1 by default.
		playerPosition = 1;
		scaleBeepDuration (1);
		mLastUpdate = System.currentTimeMillis();
		gameMode = IDLE;
		isLit = false;
		heardButtonPress = false;
		pauseDuration = 0;
		winToneIndex = 0;
		theGame = 1;
		
		for (int i = 0; i < 4; i++)  {
			activeColors[i] = true;			// Mark all colors active.
		}
		
	}
	
	void scaleBeepDuration (int index) {
		if (index < 6 ) beepDuration = 420;		 // 1 to 5 is .42s
		else if (index < 14) beepDuration = 320; // 6 to 13 is .32s
		else beepDuration = 220;				// 14 to 31 is .22s
	}
	
	private UpdateHandler mUpdateHandler = new UpdateHandler();
	
	class UpdateHandler extends Handler {
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UI: 
				SimonClone.this.update();
				break;
			case TIMEOUT: 
				SimonClone.this.gameTimeoutLose();
				break;
			}
		}
		
		public void sleep(long delayMillis) {
			this.removeMessages(UI);
			sendMessageDelayed(obtainMessage(UI), delayMillis);
		}
	}
	
	public void setLevel(int level) {
		int savedTotalLength = totalLength;
		
		switch (level) {
		case 0:
			totalLength = 8;
			break;
		case 1:
			totalLength = 14;
			break;
		case 2:
			totalLength = 20;
			break;
		case 3:
			totalLength = 30;
			break;
		default:
			totalLength = 4; 	// Should never get here.
			break;
		}
		if (totalLength != savedTotalLength) {	// If we changed the game level reset the game.
			gameClearTimeout ();
			if (pauseDuration > 0)  pauseDuration = 0;  // Go directly to idle, and don't pause.
			if (isLit) playNext();	  // If there's a button lit, turn it off.
			gameMode = IDLE;
			sequenceIndex = 0;
			// We could set sequenceLength to 0 and inhibit last, but I think not.
		}
	}
	
	public void setGame(int level) {
		theGame = level;
	}
	
	public void gameSetTimeout() {
		mUpdateHandler.removeMessages(TIMEOUT);						// Clear any old timeouts.
		if (!DISABLE_TIMEOUT)
			mUpdateHandler.sendEmptyMessageDelayed(TIMEOUT, 3000);   // Set a new 3 second timeout.
	}
	
	public void gameClearTimeout() {
		mUpdateHandler.removeMessages(TIMEOUT);						// Clear any old timeouts.		
	}
	
	public void update() {
		long now = System.currentTimeMillis();
		long delay = beepDuration;	// Events are normally the length of a beep.
		if (isLit) delay = 50;   // Usually delay 50ms after turning off a lit light.
		if (gameMode == WINNING) {  // Special delays when playing winning tone sequence.
			if (winToneIndex == 0) delay = 20; // First beep duration is .02 s.
			else delay = 70;		// Subsequent beeps are .07 s.
			if (isLit) delay = 20;	// and delay .02 s. between tones.
		}
		if (pauseDuration > 0) delay = pauseDuration; // Long delays should only happen when light is off.

		if (gameMode != LISTENING) {
			if (now - mLastUpdate > delay) {
				playNext();
				mLastUpdate = now;
			}
			mUpdateHandler.sleep(delay);
			}
	}
	
	
	public void playNext() {
		if (pauseDuration > 0) { 									// OK, we've delayed.
			pauseDuration = 0; 
			return;
		}
		switch (gameMode) {
		case REPLAYING:
		case PLAYING:  //  Play the current sequence.
			if (sequenceIndex < sequenceLength) {	// Keep playing
				if (isLit) {
					showButtonRelease(currentSequence[sequenceIndex]); // Stop previous tone.
					isLit = false;
					sequenceIndex++;								// Point at next
					if (sequenceIndex == sequenceLength) { // Played last tone.
						if (gameMode == PLAYING) {		// If we're playing begin listening for input.
							gameSetTimeout();			
							sequenceIndex = 0;			// Now use sequenceIndex as match cursor.
							gameMode = LISTENING;					/* gameMode = SET_LISTEN;	// switch to Listen when button release feedback is done. */
						} else gameMode = IDLE;							// or go to Idle state after replay.
					}
				} else {
					showButtonPress(currentSequence[sequenceIndex]);	// Flash and beep current.
					isLit = true;
				}
			} // Fall through and do nothing if we're past the end of the sequence.
			break;
		case LONG_PLAYING:  //  Play the current sequence.
			if (isLit) {
				showButtonRelease(longestSequence[sequenceIndex]); // Stop previous tone.
				isLit = false;
				sequenceIndex++;								// Point at next
				return;
			}
			if (sequenceIndex < longestLength) {
				showButtonPress(longestSequence[sequenceIndex]);	// Flash and beep current.
				isLit = true;
			} else {											// Played all
				gameMode = IDLE;
			}
			break;
		case WINNING:
			if (isLit) {
				showButtonRelease(currentSequence[sequenceLength - 1]);
				isLit = false;
				if (winToneIndex == 6) gameMode = WON;
			} else {
				showButtonPress(currentSequence[sequenceLength - 1]);
				isLit = true;
				winToneIndex++;
			}
			break;
		case LOSING:
			gameMode = LOST;
			break;
		}
	}
	
	public void playCurrent() {
		gameMode = PLAYING;
		sequenceIndex = 0;
		update();
	}
	
	public void playLast() {
		switch (gameMode) {
		case IDLE:
		case WON:
		case LOST:
			gameMode = REPLAYING;
			sequenceIndex = 0;
			update();
			break;
		default:
				return;
		}
	}
	
	public void playLongest () {
		switch (gameMode) {
		case IDLE:
		case WON:
		case LOST:
			gameMode = LONG_PLAYING;
			sequenceIndex = 0;
			update();
			break;
		default:
				return;
		}
	}
	
	public int getRandomColor () {
		int retval = RNG.nextInt(4);
		if (theGame == 3)  {			// Filter out inactive colors
			while (activeColors[retval] == false)  retval = RNG.nextInt(4);  // Keep trying till we get an active.
		}
		return retval;
	}
	
	public void gameStart() {
		for (int i = 0; i < 4; i++)  {
			activeColors[i] = true;			// Mark all colors active.
		}
		winToneIndex = 0;
		sequenceLength = 1;
		scaleBeepDuration (1);
		playerPosition = 1;
		currentSequence[0] = getRandomColor();
		playCurrent();
	}

	public void gameTest() {
		for (int i = 0; i < 6; i++) {
			currentSequence[i] = RNG.nextInt(4);
		}
		sequenceLength = 6;
		scaleBeepDuration (sequenceLength);
		playCurrent();
	}
	
	public void maintainLongest () {
		if (sequenceLength > longestLength) {
			for (int i = 0; i < sequenceLength; i++) {
				longestSequence[i] = currentSequence[i];
			}
			longestLength = sequenceLength;
		}		
	}
	
	public void gameWin() {
		mLastUpdate = System.currentTimeMillis();
		pauseDuration = 800;		// We play the winning tone .8 s. after win.
		gameMode = WINNING;
		update();
	}
	
	public void gameTimeoutLose () {
		if (theGame == 3)  {
			activeColors[currentSequence[sequenceIndex]] = false;
			Log.d(TAG, "SequenceIndex: " + sequenceIndex + " Eliminating active color: " + 
					currentSequence[sequenceIndex]);
		}
		gameLose();
	}
	public void gameLose() {
		doStream(soundIds[LOSE_SOUND]);
		if (theGame == 3) {   // In game 3 we eliminate a color and start again.
			int activeColorCount = 0;
			for (int i=0; i < 4; i++) {
				Log.d(TAG, "activeColor[" + i + "] is: " + activeColors[i]);
				if (activeColors[i]) activeColorCount++;
			}
			if (activeColorCount == 1) gameWin();
			else {
				sequenceLength = 1;
				scaleBeepDuration (1);
				currentSequence[0] = getRandomColor();
				gameCycle();
			}
		} else {
			gameMode = LOSING;
			update();
		}
	}
	
	public void gameCycle() {
		mLastUpdate = System.currentTimeMillis();
		pauseDuration = 800;		// Wait .8s after last key pressed to play next for game 1 and 3.
		playerPosition = 1;
		update();
		playCurrent();
	}

	/*
	 * pressButton is called by the Touch Handler in response to user cction
	 * We deal with the work in response to the user action and then we show
	 * that we have pressed the button.
	 */
	
	public void pressButton (int buttonIndex)  {
		if (gameMode != LISTENING) return;		// Only examine values when game is in play.
		// Guard against entering LISTENING state between a press and a release.
		heardButtonPress = true;
		// Logic for game 2:  We take user input as next color and fall through to normal case.
		if (playerPosition > sequenceLength) {
			currentSequence[sequenceIndex] = buttonIndex;
			sequenceLength++;
			playerPosition++;		// Point past new end of list and trigger restart of matching.
		}
		
		// Regular logic.
		if (currentSequence[sequenceIndex] == buttonIndex) {	// showButton only if match.
			maintainLongest();
			showButtonPress(buttonIndex);
		}
		else {
			gameClearTimeout();					// showButton Press would have done this for us.
			doStream(soundIds[LOSE_SOUND]);
			if (theGame == 3) {		// Eliminate color that was pressed in game 3.
				Log.d(TAG, "Eliminating active color: " + buttonIndex);
				activeColors[buttonIndex] = false;
			}
			gameLose();
		}
	}

	/*
	 * releaseButton is called by the Touch Handler in response to user cction
	 * We deal with the work in response to the user action and then we show
	 * that we have released the button.
	 */
	
	public void showButtonPress(int index) {
		gameClearTimeout();		// The real game has a cheat:  Timeout is suspended while pressing button.
		if (index >= 0 && index < TOTAL_BUTTONS) {
			if (buttonPressMap[index] == false) {
				buttonPressMap[index] = true;
			
				switch (gameMode) {
				case WON:
					doStream(soundIds[VICTORY_SOUND]);
					break;
				case WINNING:
					doStream(soundIds[RED]);  // Play the red sound for win.
					break;
				case LOSING: 
					doStream(soundIds[LOSE_SOUND]);
					return;
				case LISTENING: 
					if (currentSequence[sequenceIndex] == index) // When we miss we barf immediately
						doStream(soundIds[index]);
					else
						doStream(soundIds[LOSE_SOUND]);
					break;
				default: 
					doStream(soundIds[index]);
					break;
				}
				for (Listener listener : listeners) {
					listener.buttonStateChanged(index); 
				}  
			}
		}
	}

	public void releaseButton (int buttonIndex ){
		if (gameMode != LISTENING) return;
		// Guard against acting on a button press that happened before we were LISTENING.
		if (heardButtonPress == false) return;

		heardButtonPress = false;			// Reset our heardButtonPress state.
		mLastUpdate = System.currentTimeMillis();
		gameSetTimeout();
		
		if (sequenceIndex < sequenceLength) {
			if (currentSequence[sequenceIndex] == buttonIndex)  { // Matched. Continue.
				showButtonRelease(buttonIndex);			// showButton only if match.
				sequenceIndex++;
				if (sequenceIndex == sequenceLength) { 
					if (sequenceLength < totalLength) {  // Add one more.
						if (theGame == 2) {  // In game 2, user adds next item in sequence
							if (playerPosition > sequenceLength) {
								playerPosition = 1;
								sequenceIndex = 0;		// We added one. Now restart matching sequence.
							} else {
								playerPosition++;		// Set the stage for adding to sequence on next button press.
							}
						} else {
							sequenceLength++;
							playerPosition = 1;
							scaleBeepDuration (sequenceLength);
							currentSequence[sequenceIndex] = getRandomColor();
							gameCycle();
						}
					} else {  // Total win!
						gameWin();
					}					
				} else {
					playerPosition++;
				}
			} else {
				if (theGame == 3) {		// Eliminate color that was pressed in game 3.
					Log.d(TAG, "Eliminating active color: " + buttonIndex);
					activeColors[buttonIndex] = false;
				}
				gameLose ();
			}
		}
		
	}
	
	public void showButtonRelease(int index) {
		if (index >= 0 && index < TOTAL_BUTTONS) {
			if (buttonPressMap[index] == true) {
				buttonPressMap[index] = false;
					if (speakerStream != 0) {
						soundPool.stop(speakerStream);
						speakerStream = 0;
				}
				for (Listener listener : listeners) {
					listener.buttonStateChanged(index);
				} 
			}
		}
	}


	public void doStream (int soundId) {
		if (soundId != 0) {  // Don't do anything different if our soundID is invalid.
			if (speakerStream !=0) {  // Stop what we were doing.
				soundPool.stop(speakerStream);
			}
			speakerStream = soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
		} 
	}
	
	public boolean isButtonPressed(int index) {
		if (index < 0 || index > TOTAL_BUTTONS) {
			return false;
		} else {
			return buttonPressMap[index];
		}
	}
	
	public void addListener(Listener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public void releaseAllButtons() {
		for (int i = 0; i < buttonPressMap.length; ++i) {
			buttonPressMap[i] = false;
		}
		for (Listener listener : listeners) {
			listener.multipleButtonStateChanged();
		}
	}
	
	public void dispose() {
		
	}
}
