/**
 * 
 */
package com.bubblesworth.soundboard.mlpfim;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
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
	
	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		
		private final class StopOnCompletion implements OnCompletionListener {
			private int msgId;
			StopOnCompletion(int msgId) {
				this.msgId = msgId;
			}
			@Override
			public void onCompletion(MediaPlayer mp) {
				mp.release();
				stopSelf(msgId);
			}
		}
		
		@Override
		public void handleMessage(Message msg) {
			Intent intent = (Intent)msg.obj;
			Uri uri = intent.getData();
			
			try {
				AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(uri, "r");
				MediaPlayer mp = new MediaPlayer();
				mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
				afd.close();
				mp.setOnCompletionListener( new StopOnCompletion(msg.arg1) );
				mp.prepare();
				mp.start();
			} catch (IOException e) {
				Log.e(TAG, "onHandleIntent", e);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		// Start up the thread running the service.  Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		HandlerThread thread = new HandlerThread(TAG+"HandlerThread", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		
		// Get the HandlerThread's Looper and use it for our Handler 
		looper = thread.getLooper();
		handler = new ServiceHandler(looper);
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		looper.quit();
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Message msg = handler.obtainMessage();
		// Had to pull this out of IntentService.java, the documentation omits this line.
		msg.obj = intent;
		msg.arg1 = startId;
		handler.sendMessage(msg);
		return START_NOT_STICKY;
	}
}

