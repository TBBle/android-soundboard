/**
 * 
 */
package com.bubblesworth.soundboard.ringtones;

import android.app.ListActivity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

/**
 * @author paulh
 * 
 */
public class SoundboardRingtoneSetterActivity extends ListActivity {
	// private static final String TAG = "SoundboardRingtoneSetterActivity";

	private static final String STATEKEY_WASRUNNING = "com.bubblesworth.soundboard.ringtones.SoundboardRingtoneSetterActivity.WASRUNNING";
	private static final String STATEKEY_MEDIAURI = "com.bubblesworth.soundboard.ringtones.SoundboardRingtoneSetterActivity.MEDIAURI";

	private Uri mediaUri = null;

	private String[] dialogLabels = new String[3];

	// Must match dialogLabels
	private int[] dialogTypes = { RingtoneManager.TYPE_RINGTONE,
			RingtoneManager.TYPE_NOTIFICATION, RingtoneManager.TYPE_ALARM, };

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Must match dialogTypes!
		dialogLabels[0] = getResources().getString(R.string.type_ringtone);
		dialogLabels[1] = getResources().getString(R.string.type_notification);
		dialogLabels[2] = getResources().getString(R.string.type_alarm);

		// If we were killed, we already started this activity. If we had a
		// mediaUri,
		// then we were showing the dialog, otherwise we were waiting for a
		// result
		if (savedInstanceState != null
				&& savedInstanceState.getBoolean(STATEKEY_WASRUNNING)) {
			mediaUri = savedInstanceState.getParcelable(STATEKEY_MEDIAURI);
			if (mediaUri != null) {
				showTypeList();
			}
			return;
		}

		// Allow callers to specify the type they wish to add.
		int type = getIntent().getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
				0);

		Uri trackUri = getIntent().getData();
		if (trackUri != null) {
			// A soundboard is giving us its track to add
			mediaUri = SoundboardRingtoneFileManager
					.getMediaUri(this, trackUri);
			if (mediaUri == null) {
				Toast.makeText(
						this,
						getResources().getText(
								R.string.toast_failed_to_store_ringtone),
						Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
			if (type != 0) {
				// The soundboard specified the type, just do the conversion
				// and we're done.
				SoundboardRingtoneFileManager.setMediaUriAsRingtone(this,
						mediaUri, type);
				finish();
				return;
			}
			showTypeList();
			return;
		}

		mediaUri = null;
		// We've been asked to set a ringtone, but not specified which one
		Intent picker = new Intent();
		picker.setClass(this, SoundboardRingtonePickerActivity.class);
		startActivityForResult(picker, 0);
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
		outState.putParcelable(STATEKEY_MEDIAURI, mediaUri);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != RESULT_OK) {
			finish();
			return;
		}
		mediaUri = data
				.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
		showTypeList();
	}

	private void showTypeList() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1,
				dialogLabels);

		setListAdapter(adapter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView,
	 * android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		int type = dialogTypes[position];
		SoundboardRingtoneFileManager.setMediaUriAsRingtone(this, mediaUri,
				type);
		finish();
	}
}