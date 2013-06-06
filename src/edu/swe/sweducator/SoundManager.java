package edu.swe.sweducator;
import java.io.FileNotFoundException;
import java.util.HashMap;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.util.Log;

public class SoundManager {
	protected static final String LOG_TAG = "SoundManager";

	//SoundPool playback constants
	private static final int STREAM_ERROR = 0;
	private static final float RATE = 1f;
	private static final int LOOP = 0;
	private static final int PRIORITY = 1;
	private static final float RIGHT_VOLUME = 1f;
	private static final float LEFT_VOLUME = 1f;

	private SoundPool soundPool;

	/** This hashmap maps soundfiles to soundId's. */
	private HashMap<String, Integer> soundMap;
	private final AssetManager assetManager;

	public SoundManager(AssetManager assetManager) {
		this.assetManager = assetManager;
		soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
		soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				if (status == 0 && sampleId != 0) {
					if (soundPool.play(sampleId, LEFT_VOLUME, 
							RIGHT_VOLUME, PRIORITY, LOOP, 
								RATE) == STREAM_ERROR) {
						Log.e(LOG_TAG, "Playback error for file with " +
								"soundId " + sampleId);
					}
				} else {
					Log.e(LOG_TAG, "Soundpool error, status code: " + status);
				}
			}
		});
		
		soundMap = new HashMap<String, Integer>();
	}
	
	public boolean playSoundFile(String filename) {
		boolean playbackSuccess = true;
		try {
			int soundID = 0;
			try {
				soundID = soundMap.get(filename);
				if (soundID == 0) {
					throw new Exception("Error loading soundfile " + filename);
				}
				if (soundPool.play(soundID, LEFT_VOLUME, RIGHT_VOLUME, 
						PRIORITY, LOOP, RATE) == STREAM_ERROR) {
					throw new Exception("Playback error for file " + 
						filename + " with soundId " + soundID);
				}
				Log.d(LOG_TAG, "Played file: " + filename);
			} catch (NullPointerException e) {
				soundID = loadSound(filename);
				if (soundID == -1) {
					playbackSuccess = false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			playbackSuccess = false;
		}
		return playbackSuccess;
	}

	private int loadSound(String filename) {
		AssetFileDescriptor afd;
		int result = 0;
		try {
			afd = assetManager.openFd(filename);
			int soundId = soundPool.load(afd, PRIORITY);
			if (soundId != 0) {
				soundMap.put(filename, soundId);
				return soundId;
			} else {
				Log.e(LOG_TAG, "Error loading file " + filename + 
						" into soundpool");
			}
		} catch (FileNotFoundException e) {
			Log.e(LOG_TAG, "File " + filename + " not found");
			result = -1;
		} catch (Exception e) {
			e.printStackTrace();
			result = -1;
		}
		return result;
	}
}