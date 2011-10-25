/**
 * 
 */
package com.bubblesworth.soundboard.mlpfim;

import java.io.IOException;
import java.util.HashSet;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

/**
 * @author tbble
 * 
 */
public class SoundboardService extends Service implements OnPreparedListener,
		OnCompletionListener {
	private static final String TAG = "SoundboardService";

	private Looper looper;
	private Handler handler;
	int lastStartId;
	private HashSet<MediaPlayer> players;

	private class PlayerStarter implements Runnable {
		Uri mediaUri;

		PlayerStarter(Uri uri) {
			mediaUri = uri;
		}

		@Override
		public void run() {
			try {
				MediaPlayer mp = new MediaPlayer();
				players.add(mp);
				mp.setDataSource(SoundboardService.this, mediaUri);
				mp.setOnCompletionListener(SoundboardService.this);
				mp.setOnPreparedListener(SoundboardService.this);
				mp.prepareAsync();
			} catch (IOException e) {
				Log.e(TAG, "PlayerStarter.run", e);
			}
		}
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		mp.start();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		mp.stop();
		// Wait 500ms for sounds to clear the hardware
		// Bluetooth devices have been observed reporting
		// completion up to 200ms early, and users are
		// reporting similar symptoms on their devices
		handler.postDelayed(new PlayerRemover(mp), 500);
	}

	private class PlayerRemover implements Runnable {
		MediaPlayer player;

		PlayerRemover(MediaPlayer mp) {
			player = mp;
		}

		@Override
		public void run() {
			player.release();
			players.remove(player);
			if (players.isEmpty()) {
				stopSelfResult(lastStartId);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		// Start up the thread running the service. Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block. We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		players = new HashSet<MediaPlayer>();
		HandlerThread thread = new HandlerThread(TAG + "HandlerThread",
				Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		looper = thread.getLooper();
		handler = new Handler(looper);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		looper.quit();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		lastStartId = startId;
		handler.post(new PlayerStarter(intent.getData()));
		return START_NOT_STICKY;
	}
}
