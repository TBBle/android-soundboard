package com.bubblesworth.soundboard.ringtones;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class SoundboardRingtonePickerActivity extends Activity {
	private static final String TAG = "SoundboardRingtonePickerActivity";

	private static final String STATEKEY_WASRUNNING = "com.bubblesworth.soundboard.ringtones.SoundboardRingtonePickerActivity.WASRUNNING";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		// If we were killed, we already started this activity, and can just
		// wait for the result.
		if (savedInstanceState == null
				|| !savedInstanceState.getBoolean(STATEKEY_WASRUNNING)) {
			// Take advantage of the Android activity selection code,
			// any compatible soundboard should respond to this action with a
			// ContentProvider URL.
			// For historical reasons, this is APPWIDGET_CONFIGURE. >_<
			Intent intent = new Intent(
					"com.bubblesworth.soundboard.APPWIDGET_CONFIGURE");
			try {
				startActivityForResult(intent, 0);
			} catch (ActivityNotFoundException e) {
				// TODO: Show the user something informational here.
				Log.e(TAG, "No activities for " + intent, e);
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

		Uri mediaUri = SoundboardRingtoneFileManager.getMediaUri(this,
				data.getData());

		if (mediaUri == null) {
			finish();
			return;
		}

		Intent resultValue = new Intent();
		resultValue.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
				mediaUri);
		setResult(RESULT_OK, resultValue);
		finish();
	}
}