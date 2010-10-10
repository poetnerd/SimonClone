package com.poetnerd.simonclone;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.os.SystemClock;
import android.os.Handler;
import android.os.Message;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public final class SimonClone {

	public static final int TOTAL_BUTTONS = 3 * 3;
	
	public interface Listener {
		void buttonStateChanged(int index);

		void multipleButtonStateChanged();
	}
	
	/* Game States for controlling action of update. */
	
	private static final int IDLE = 0;
	private static final int LISTENING = 1;
	private static final int PLAYING = 2;
	private static final int WINNING = 4;
	private static final int LOSING = 5;
	private static final int WON = 6;
	private static final int LOST = 7;
	
	/* Names for the sounds we make */
	
	private static final int GREEN_SOUND = 0;
	private static final int RED_SOUND = 1;
	private static final int YELLOW_SOUND = 2;
	private static final int BLUE_SOUND = 3;
	private static final int VICTORY_SOUND = 4;
	private static final int LOSE_SOUND = 5;
	private static final int SPECIAL_RAZZ = 6;
	
	private int[] longestSequence = new int[32];
	private int[] currentSequence = new int[32];
	
	private int longestLength;
	private int sequenceLength;
	private int sequenceIndex;
	private int totalLength;
	private long beepDuration;
	private long mLastUpdate;
	private int gameMode;
	
	private static final Random RNG = new Random();
	private boolean isOn;
	private boolean doPause;
	
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
		soundIds[GREEN_SOUND] = soundPool.load(context, R.raw.green_long, 1);
		soundIds[RED_SOUND] = soundPool.load(context, R.raw.red_long, 1);
		soundIds[YELLOW_SOUND] = soundPool.load(context, R.raw.yellow_long, 1);
		soundIds[BLUE_SOUND] = soundPool.load(context, R.raw.blue_long, 1);
		soundIds[VICTORY_SOUND] = soundPool.load(context, R.raw.victory, 1);
		soundIds[LOSE_SOUND] = soundPool.load(context, R.raw.lose, 1);
		soundIds[SPECIAL_RAZZ] = soundPool.load(context, R.raw.special_razz, 1);

		
		longestLength = 0;
		sequenceLength = 0;
		sequenceIndex = 0;
		totalLength = 3;  // Go easy for test.
		beepDuration = 220;
		mLastUpdate = System.currentTimeMillis();
		gameMode = IDLE;
		isOn = false;
		doPause = false;
		
	}
	
	private UpdateHandler mUpdateHandler = new UpdateHandler();
	
	class UpdateHandler extends Handler {
		
		@Override
		public void handleMessage(Message msg) {
			SimonClone.this.update();
		}
		public void sleep(long delayMillis) {
			this.removeMessages(0);
			sendMessageDelayed(obtainMessage(0), delayMillis);
		}
	}
	
	public void update() {
		long now = System.currentTimeMillis();
		long delay = beepDuration;	// Events are normally the length of a beep.
		if (isOn) delay = 50;   // 50ms to turn off a lit light.
		if (doPause) delay = 800; // Long delays should only happen when light is off.
		if (gameMode != LISTENING) {
			if (now - mLastUpdate > delay) {
				playNext();
				mLastUpdate = now;
			}
			mUpdateHandler.sleep(delay);
			}
	}
	
	
	public void playNext() {
		if (doPause) { 									// OK, we've delayed.
			doPause = false; 
			return;
		}
		switch (gameMode) {
		case PLAYING:  // Normal mode: Play the sequence.
			if (isOn) {
				doReleaseButton(currentSequence[sequenceIndex]); // Stop previous tone.
				isOn = false;
				sequenceIndex++;								// Point at next
				return;
			}
			if (sequenceIndex < sequenceLength) {
				pressButton(currentSequence[sequenceIndex]);	// Flash and beep current.
				isOn = true;
			} else {											// Played all; Listen for buttons.
				gameMode = LISTENING;
				sequenceIndex = 0;								// Match on first in sequence.
			}
			break;
		case WINNING:
			if (isOn) {
				doReleaseButton(currentSequence[sequenceLength - 1]);
				isOn = false;
				gameMode = WON;
			} else {
				pressButton(currentSequence[sequenceLength - 1]);
				isOn = true;
			}
			break;
		case LOSING:
			gameMode = LOST;
			break;
		}
	}
	
	public void playLongest() {
		
	}
	
	public void playCurrent() {
		gameMode = PLAYING;
		sequenceIndex = 0;
		update();
	}
	
	public void playLast() {
		if (gameMode == IDLE) {
			gameMode = PLAYING;
			sequenceIndex = 0;
			update();
		}
	}
	
	public void gameStart() {
		sequenceLength = 1;
		currentSequence[0] = RNG.nextInt(4);
		playCurrent();
	}

	public void gameTest() {
		for (int i = 0; i < 6; i++) {
			currentSequence[i] = RNG.nextInt(4);
		}
		sequenceLength = 6;
		playCurrent();
	}
	
	public void gameWin() {
	/*	mLastUpdate = System.currentTimeMillis();
		doPause = true; */
		gameMode = WINNING;
		update();
	}
	
	public void gameLose() {
		mLastUpdate = System.currentTimeMillis();
		doPause = true;
		gameMode = LOSING;
		update();
	}
	
	public void gameCycle() {
		mLastUpdate = System.currentTimeMillis();
		doPause = true;
		update();
		playCurrent();
	}

	public void releaseButton (int buttonIndex ){
		doReleaseButton(buttonIndex);
		mLastUpdate = System.currentTimeMillis();

		if (gameMode != LISTENING) return;		// Only examine values when game is in play.
		
		if (sequenceIndex < sequenceLength) {
			if (currentSequence[sequenceIndex] == buttonIndex)  { // Matched. Continue.
				sequenceIndex++;
				if (sequenceIndex == sequenceLength) { 
					if (sequenceLength < totalLength) {  // Add one more.
						sequenceLength++;
						currentSequence[sequenceIndex] = RNG.nextInt(4);
						gameCycle();
					} else {  // Total win!
						gameWin();
					}					
				}
			} else {
				gameLose ();
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
	
	public void pressButton(int index) {
		if (index >= 0 && index < TOTAL_BUTTONS) {
			if (buttonPressMap[index] == false) {
				buttonPressMap[index] = true;
			
				switch (gameMode) {
				case WON:
					doStream(soundIds[VICTORY_SOUND]);
					break;
				case WINNING:
					doStream(soundIds[SPECIAL_RAZZ]);
					break;
				case LOSING: 
				case LOST:
					doStream(soundIds[LOSE_SOUND]);
					break;
				default: 
					if (currentSequence[sequenceIndex] == index) // When we miss we barf immediately
						doStream(soundIds[index]);
					else
						doStream(soundIds[LOSE_SOUND]);
					break;
				}
				for (Listener listener : listeners) {
					listener.buttonStateChanged(index); 
				}  
			}
		}
	}
	
	public void doReleaseButton(int index) {
		if (index >= 0 && index < TOTAL_BUTTONS) {
			if (buttonPressMap[index] == true) {
				buttonPressMap[index] = false;

				switch (gameMode) {
				case WINNING:
				case LOSING:
					break;
				case LISTENING:
				case PLAYING:
					if (speakerStream != 0) {
						soundPool.stop(speakerStream);
						speakerStream = 0;
						break;
					}
				}
				for (Listener listener : listeners) {
					listener.buttonStateChanged(index);
				} 
			}
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
