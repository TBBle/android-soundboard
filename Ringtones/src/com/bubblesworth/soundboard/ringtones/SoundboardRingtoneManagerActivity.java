/**
 * 
 */
package com.bubblesworth.soundboard.ringtones;

import android.app.ListActivity;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * @author paulh
 * 
 */
public class SoundboardRingtoneManagerActivity extends ListActivity {
	// private static final String TAG = "SoundboardRingtoneManagerActivity";

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String[] columns = { MediaStore.Audio.Media._ID,
				MediaStore.Audio.Media.TITLE };
		Cursor cur = SoundboardRingtoneFileManager.query(this, columns,
				MediaStore.Audio.Media.TITLE + " ASC");
		this.startManagingCursor(cur);
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1, cur,
				new String[] { MediaStore.Audio.Media.TITLE },
				new int[] { android.R.id.text1 });
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
		Uri mediaUri = ContentUris.withAppendedId(
				SoundboardRingtoneFileManager.CONTENT_URI, id);
		SoundboardRingtoneFileManager.deleteMediaUri(this, mediaUri);
	}

}
