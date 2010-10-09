package com.poetnerd.simonclone;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.os.SystemClock;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public final class SimonClone {

	public static final int TOTAL_BUTTONS = 3 * 3;
	
	public interface Listener {
		void buttonStateChanged(int index);

		void multipleButtonStateChanged();
	}
	
	private int[] longestSequence = new int[32];
	private int[] currentSequence = new int[32];
	
	private int longestLength;
	private int sequenceLength;
	private int beepDuration;
	
	private static final Random RNG = new Random();
	
	private boolean[] buttonPressMap = new boolean[TOTAL_BUTTONS];
	
	private List<Listener> listeners = new ArrayList<Listener>();
	
	private SoundPool soundPool;
	
	private int[] soundIds = new int[TOTAL_BUTTONS];
	
	private int[] streamIds = new int[TOTAL_BUTTONS];
	
	public SimonClone(Context context) {
		for (int i = 0; i < TOTAL_BUTTONS; ++i) {
			buttonPressMap[i] = false;
		}
		
		soundPool = new SoundPool(TOTAL_BUTTONS, AudioManager.STREAM_MUSIC, 0);
		soundIds[0] = soundPool.load(context, R.raw.green_long, 1);
		soundIds[1] = soundPool.load(context, R.raw.red_long, 1);
		soundIds[2] = soundPool.load(context, R.raw.yellow_long, 1);
		soundIds[3] = soundPool.load(context, R.raw.blue_long, 1);
		
		longestLength = 0;
		sequenceLength = 0;
		beepDuration = 440;
		longestSequence[0] = 4;
		currentSequence[0] = 4;
		
		for (int i=0; i < 4; i++) {
			longestSequence[i] = RNG.nextInt(4);
		}
		longestSequence[4] = 4;
	}
	
	public void playLongest() {
		int i = 0;
		while (longestSequence[i] != 4) {
			this.pressButton (longestSequence[i]);
			SystemClock.sleep(beepDuration);
			this.releaseButton(longestSequence[i]);
			i++;
		}
	}

	public void playSequence() {
		int i = 0;
		while (currentSequence[i] != 4) {
			pressButton (currentSequence[i]);
		}
	}
	

	public void pressButton(int index) {
		if (index >= 0 && index < TOTAL_BUTTONS) {
			if (buttonPressMap[index] == false) {
				buttonPressMap[index] = true;
				
				int soundId = soundIds[index];
				if (soundId != 0) {
					streamIds[index] = soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
				}
				
				for (Listener listener : listeners) {
					listener.buttonStateChanged(index);
				}
			}
		}
	}
	
	public void releaseButton(int index) {
		if (index >= 0 && index < TOTAL_BUTTONS) {
			if (buttonPressMap[index] == true) {
				buttonPressMap[index] = false;
	
				int streamId = streamIds[index];
				if (streamId != 0) {
					soundPool.stop(streamId);
					streamIds[index] = 0;
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
