/**
 * 
 */
package com.bubblesworth.soundboard.ringtones;

import java.io.IOException;

import android.app.ListActivity;
import android.content.ContentUris;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * @author paulh
 * 
 */
public class SoundboardRingtoneManagerActivity extends ListActivity {
	private static final String TAG = "SoundboardRingtoneManagerActivity";

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		SoundboardRingtoneFileManager.validateMediaStore(this);
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
		registerForContextMenu(getListView());
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
		MediaPlayer mp = new MediaPlayer();
		mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				mp.release();
			}
		});
		try {
			mp.setDataSource(this, mediaUri);
			mp.prepare();
		} catch (IOException e) {
			Log.e(TAG, "Failed to play back mediaUri " + mediaUri.toString());
		}
		mp.start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
	 * android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.manager_context_menu, menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		Uri mediaUri = ContentUris.withAppendedId(
				SoundboardRingtoneFileManager.CONTENT_URI, info.id);
		switch (item.getItemId()) {
		case R.id.menuDelete:
			SoundboardRingtoneFileManager.deleteMediaUri(this, mediaUri);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

}
