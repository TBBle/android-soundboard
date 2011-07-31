package com.bubblesworth.soundboard.mlpfim;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.bubblesworth.soundboard.SoundColumns;

/**
 * @author paulh
 *
 */
public class SoundChooserActivity extends ListActivity {
	//private static final String TAG = "SoundChooserActivity";

	private boolean widgetConfig;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		widgetConfig = getIntent().getAction().equals("com.bubblesworth.soundboard.APPWIDGET_CONFIGURE");
		String[] columns = {SoundColumns._ID, SoundColumns.DESCRIPTION, SoundColumns.ICON};
		Cursor cur = managedQuery(SoundProvider.TRACK_URI, columns, null, null, null);
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.icon_list_item,
				cur,
				new String[] {SoundColumns.DESCRIPTION, SoundColumns.ICON},
				new int[] {R.id.listText, R.id.listIcon}
				);
		setListAdapter(adapter);
		if (widgetConfig) {
			setResult(RESULT_CANCELED);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Uri uri = ContentUris.withAppendedId(SoundProvider.TRACK_URI, id);
		if (widgetConfig) {
			Intent resultValue = new Intent();
			resultValue.setData(uri);
			setResult(RESULT_OK, resultValue);
			finish();
		} else {
			String[] columns = {SoundColumns.ACTION, SoundColumns.ASSET};
			Cursor cur = managedQuery(uri, columns, null, null, null);
			if (cur.moveToFirst()) {
				String action = cur.getString(cur.getColumnIndex(SoundColumns.ACTION));
				String asset = cur.getString(cur.getColumnIndex(SoundColumns.ASSET));
				Intent intent = new Intent(action);
				intent.setData(Uri.parse(asset));
				startService(intent);
			}
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (!widgetConfig) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.chooser_menu, menu);
		}
		return !widgetConfig;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuAbout:
			startActivity(new Intent( this, com.bubblesworth.soundboard.mlpfim.AboutActivity.class ));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
