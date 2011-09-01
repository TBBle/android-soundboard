/**
 * 
 */
package com.bubblesworth.soundboard.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * @author tbble
 * 
 */
public class SoundAppWidgetConfigActivity extends Activity {
	private static final String TAG = "SoundAppWidgetConfigActivity";

	private static final String STATEKEY_WASRUNNING = "com.bubblesworth.soundboard.widget.SoundAppWidgetConfigActivity.WASRUNNING";

	private int widgetId;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		setResult(RESULT_CANCELED);
		if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
			return;
		}

		// If we were killed, we already started this activity, and can just
		// wait for the result.
		if (savedInstanceState == null
				|| !savedInstanceState.getBoolean(STATEKEY_WASRUNNING)) {
			// Take advantage of the Android activity selection code,
			// any compatible soundboard should respond to this action with a
			// ContentProvider URL.
			// I wonder if or how I can export this...? I guess you just
			// document
			// it, since APPWIDGET_CONFIG isn't exported from
			// android.appwidget.action
			// either.
			Intent intent = new Intent(
					"com.bubblesworth.soundboard.APPWIDGET_CONFIGURE");
			try {
				startActivityForResult(intent, 0);
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "No activities for " + intent, e);
				Toast.makeText(
						this,
						getResources().getText(
								R.string.toast_no_supported_soundboards),
						Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STATEKEY_WASRUNNING, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK) {
			finish();
			return;
		}
		Uri asset = data.getData();
		storeContentURIForWidget(this, widgetId, asset);
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		SoundAppWidgetProvider.updateAppWidget(this, appWidgetManager,
				widgetId, asset);

		// Notify the AppWidgetManager that we finished configuring the widget
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		setResult(RESULT_OK, resultValue);
		finish();
	}

	private static final String PREFS_NAME = "com.bubblesworth.soundboard.widget";
	private static final String PREF_WIDGET_ASSET_KEY = "widget_asset_";

	// This is actually called from SoundAppWidgetProvider, but it's here
	// because
	// it relies on the private data above.
	// Hopefully this doesn't fail due to Context mismatch?
	static Uri retreiveContentURIForWidget(Context context, int appWidgetId) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME,
				MODE_PRIVATE);
		String asset = prefs.getString(PREF_WIDGET_ASSET_KEY + appWidgetId,
				null);
		if (asset == null)
			return null;
		return Uri.parse(asset);
	}

	static void storeContentURIForWidget(Context context, int appWidgetId,
			Uri asset) {
		SharedPreferences.Editor prefs = context.getSharedPreferences(
				PREFS_NAME, 0).edit();
		prefs.putString(PREF_WIDGET_ASSET_KEY + appWidgetId, asset.toString());
		prefs.commit();
	}
}
