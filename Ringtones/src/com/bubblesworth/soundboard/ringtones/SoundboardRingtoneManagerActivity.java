/**
 * 
 */
package com.bubblesworth.soundboard.ringtones;

import java.io.IOException;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * @author paulh
 * 
 */
public class SoundboardRingtoneManagerActivity extends ListActivity {
	private static final String TAG = "SoundboardRingtoneManagerActivity";

	private class RingtoneManagerViewBinder implements
			SimpleCursorAdapter.ViewBinder {

		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if (columnIndex == cursor
					.getColumnIndex(MediaStore.Audio.Media._ID)
					&& view.getId() == android.R.id.text2) {
				Uri mediaUri = ContentUris.withAppendedId(
						SoundboardRingtoneFileManager.CONTENT_URI,
						cursor.getInt(cursor
								.getColumnIndex(MediaStore.Audio.Media._ID)));
				String type = "";
				if (mediaUri.equals(RingtoneManager
						.getActualDefaultRingtoneUri(
								SoundboardRingtoneManagerActivity.this,
								RingtoneManager.TYPE_RINGTONE))) {
					if (type != "")
						type += ", ";
					type += getResources().getString(R.string.type_ringtone);
				}
				if (mediaUri.equals(RingtoneManager
						.getActualDefaultRingtoneUri(
								SoundboardRingtoneManagerActivity.this,
								RingtoneManager.TYPE_NOTIFICATION))) {
					if (type != "")
						type += ", ";
					type += getResources()
							.getString(R.string.type_notification);
				}
				if (mediaUri.equals(RingtoneManager
						.getActualDefaultRingtoneUri(
								SoundboardRingtoneManagerActivity.this,
								RingtoneManager.TYPE_ALARM))) {
					if (type != "")
						type += ", ";
					type += getResources().getString(R.string.type_alarm);
				}
				if (view instanceof TextView) {
					((TextView) view).setText(type);
				} else {
					throw new IllegalStateException(view.getClass().getName()
							+ " is not a TextView");

				}
				return true;
			}
			return false;
		}
	}

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
				android.R.layout.simple_list_item_2, cur, new String[] {
						MediaStore.Audio.Media.TITLE,
						MediaStore.Audio.Media._ID }, new int[] {
						android.R.id.text1, android.R.id.text2 });
		adapter.setViewBinder(new RingtoneManagerViewBinder());
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
		case R.id.menuRingtone:
			SoundboardRingtoneFileManager.setMediaUriAsRingtone(this, mediaUri,
					RingtoneManager.TYPE_RINGTONE);
			((SimpleCursorAdapter) this.getListAdapter())
					.notifyDataSetChanged();
			return true;
		case R.id.menuNotification:
			SoundboardRingtoneFileManager.setMediaUriAsRingtone(this, mediaUri,
					RingtoneManager.TYPE_NOTIFICATION);
			((SimpleCursorAdapter) this.getListAdapter())
					.notifyDataSetChanged();
			return true;
		case R.id.menuAlarm:
			SoundboardRingtoneFileManager.setMediaUriAsRingtone(this, mediaUri,
					RingtoneManager.TYPE_ALARM);
			((SimpleCursorAdapter) this.getListAdapter())
					.notifyDataSetChanged();
			return true;
		case R.id.menuDelete:
			SoundboardRingtoneFileManager.deleteMediaUri(this, mediaUri);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.manager_options_menu, menu);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuAdd:
			Intent setter = new Intent();
			setter.setClass(this, SoundboardRingtoneSetterActivity.class);
			startActivityForResult(setter, 0);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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
		((SimpleCursorAdapter) this.getListAdapter()).notifyDataSetChanged();
	}
}