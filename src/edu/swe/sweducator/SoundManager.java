package edu.swe.sweducator;
import java.io.IOException;
import java.util.HashMap;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.util.Log;

public class SoundManager {
	protected static final String LOG_TAG = "SoundManager";
	private SoundPool sp;
	private HashMap<String, Integer> soundMap;
	private final AssetManager assetManager;

	public SoundManager(AssetManager assetManager) {
		this.assetManager = assetManager;
		soundMap = new HashMap<String, Integer>();
		sp = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
		sp.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				Log.d(LOG_TAG, "Sound loaded");
			}
		});
	}

	public void playSoundFileByName(String fileName) {
		if ("i.ogg".equals(fileName) == false) {
			Log.d(LOG_TAG, "Wrong file: " + fileName);
			return;
		}
		try {
			if (!soundMap.containsKey(fileName)) {
				AssetFileDescriptor afd = assetManager.openFd(fileName);
				int soundID = sp.load(afd, 1);
				soundMap.put(fileName, soundID);
			}
			
			if (sp.play(soundMap.get(fileName), 1f, 1f, 1, 0, 1f) == 0) {
				throw new Exception();
			}
			
			Log.d(LOG_TAG, "Played file: " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}