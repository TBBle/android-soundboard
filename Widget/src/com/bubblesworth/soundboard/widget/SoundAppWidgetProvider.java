/**
 * 
 */
package com.bubblesworth.soundboard.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * @author tbble
 *
 */
public class SoundAppWidgetProvider extends AppWidgetProvider {
	//private static final String TAG = "SoundAppWidgetProvider";

	/* (non-Javadoc)
	 * @see android.appwidget.AppWidgetProvider#onUpdate(android.content.Context, android.appwidget.AppWidgetManager, int[])
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		final int count = appWidgetIds.length;
		//Log.d(TAG, "onUpdate for " + count + " widgets in " + context);
		for (int i = 0; i < count; ++i) {
			int appWidgetId = appWidgetIds[i];
			Uri asset = SoundAppWidgetConfigActivity.retreiveContentURIForWidget(context, appWidgetId);
			updateAppWidget(context, appWidgetManager, appWidgetId, asset);
		}
	}

	static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
			int appWidgetId, Uri uri) {
		if (uri == null)
			return;
		// Fire up a thread to do the setup for the relevant AppWidget once
		// the ContentProvider is available.
		SoundAppWidgetUpdater updater = new SoundAppWidgetUpdater( context, appWidgetManager, appWidgetId, uri );
		new Thread(updater, SoundAppWidgetUpdater.TAG + "_" + appWidgetId).start();
	}

	static private class SoundAppWidgetUpdater implements Runnable {
		private static final String TAG = "SoundAppWidgetUpdater";

		// TODO: Copied from SoundProvider.java. They should be in an interface or something.
		private static final String ACTION = "action";
		private static final String ASSET = "asset";

		private static final int cpQueryRepeat = 5000;

		private Context context;
		private AppWidgetManager appWidgetManager;
		private int appWidgetId;
		private Uri uri;

		public SoundAppWidgetUpdater(Context context, AppWidgetManager appWidgetManager,
				int appWidgetId, Uri uri) {
			this.context = context;
			this.appWidgetManager = appWidgetManager;
			this.appWidgetId = appWidgetId;
			this.uri = uri;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			// Query the ContentProvider at the Uri for the "action" and "asset" tags
			//Log.d(TAG, "getContentResolver#"  + appWidgetId + ": " + uri.toString());
			String[] columns = {ACTION, ASSET};
			Cursor cur;
			do {
				// TODO: Give up eventually...
				// Can we detect that no ContentResolver will ever exist?
				cur = context.getContentResolver().query(uri, columns, null, null, null);
				//Log.d(TAG, "getContentResolver#"  + appWidgetId + " -> " + cur);
				if (cur == null) {
					try {
						Thread.sleep(cpQueryRepeat);
					} catch (InterruptedException e) {
						Log.e(TAG, "Thread.sleep#"  + appWidgetId, e);
					}
				}
			} while (cur == null);

			if (cur.moveToFirst()) {
				String action = cur.getString(cur.getColumnIndex(ACTION));
				String asset = cur.getString(cur.getColumnIndex(ASSET));
				Intent intent = new Intent(action);
				intent.setData(Uri.parse(asset));
				PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
				if (pendingIntent == null)
					return;
				RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.soundappwidget);
				views.setOnClickPendingIntent(R.id.widgetIcon, pendingIntent);
				appWidgetManager.updateAppWidget(appWidgetId, views);
			}
			cur.close();
		}
	}
}
