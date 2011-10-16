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
import android.os.Message;
import android.os.Process;
import android.util.Log;

/**
 * @author tbble
 * 
 */
public class SoundboardService extends Service {
	private static final String TAG = "SoundboardService";

	private Looper looper;
	private ServiceHandler handler;
	int lastStartId;

	private final class ServiceHandler extends Handler implements
			OnPreparedListener, OnCompletionListener {
		private HashSet<MediaPlayer> players;

		public ServiceHandler(Looper looper) {
			super(looper);
			players = new HashSet<MediaPlayer>();
		}

		@Override
		public void handleMessage(Message msg) {
			Intent intent = (Intent) msg.obj;
			Uri uri = intent.getData();

			try {
				MediaPlayer mp = new MediaPlayer();
				players.add(mp);
				mp.setDataSource(SoundboardService.this, uri);
				mp.setOnCompletionListener(this);
				mp.setOnPreparedListener(this);
				mp.prepareAsync();
			} catch (IOException e) {
				Log.e(TAG, "onHandleIntent", e);
			}
		}

		@Override
		public void onPrepared(MediaPlayer mp) {
			mp.start();
		}

		@Override
		public void onCompletion(MediaPlayer mp) {
			mp.release();
			players.remove(mp);
			if (players.isEmpty())
				stopSelfResult(lastStartId);
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
		HandlerThread thread = new HandlerThread(TAG + "HandlerThread",
				Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		looper = thread.getLooper();
		handler = new ServiceHandler(looper);
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
		Message msg = handler.obtainMessage();
		// Had to pull this out of IntentService.java, the documentation omits
		// this line.
		msg.obj = intent;
		lastStartId = startId;
		handler.sendMessage(msg);
		return START_NOT_STICKY;
	}
}
