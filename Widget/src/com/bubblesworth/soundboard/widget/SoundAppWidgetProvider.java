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
import android.view.View;
import android.widget.RemoteViews;

import com.bubblesworth.soundboard.SoundColumns;

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
			String[] columns = {SoundColumns.ACTION, SoundColumns.ASSET, SoundColumns.ICON, SoundColumns.DESCRIPTION};
			Cursor cur;
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.soundappwidget);
			views.setImageViewResource(R.id.widgetIcon, R.drawable.loading);
			appWidgetManager.updateAppWidget(appWidgetId, views);
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

			views.setImageViewResource(R.id.widgetIcon, R.drawable.error);
			if (cur.moveToFirst()) {
				Intent intent = new Intent();
				try {
					intent.setAction(cur.getString(cur.getColumnIndexOrThrow(SoundColumns.ACTION)));
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "Missing ACTION column", e);
					return;
				}

				try {
					intent.setData(Uri.parse(
							cur.getString(cur.getColumnIndexOrThrow(SoundColumns.ASSET))
							));
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "Missing ASSET column", e);
					return;
				}

				PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
				if (pendingIntent == null)
					return;

				views.setOnClickPendingIntent(R.id.widgetIcon, pendingIntent);

				try {
					views.setImageViewUri(R.id.widgetIcon, Uri.parse(
							cur.getString(cur.getColumnIndexOrThrow(SoundColumns.ICON))
							));
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "Missing ICON column", e);
					views.setImageViewResource(R.id.widgetIcon, R.drawable.noicon);
				}
				try {
					String description = cur.getString(cur.getColumnIndexOrThrow(SoundColumns.DESCRIPTION));
					if (description.length() > 0) {
						views.setTextViewText(R.id.widgetLabel, description);
						views.setViewVisibility(R.id.widgetLabel, View.VISIBLE );
					}
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "Missing DESCRIPTION column", e);
				}
			}
			cur.close();
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}
}
