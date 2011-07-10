package com.bubblesworth.soundboard.mlpfim;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class SoundChooserActivity extends ListActivity {
	//private static final String TAG = "SoundChooserActivity";

	private boolean widgetConfig;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		widgetConfig = getIntent().getAction().equals("com.bubblesworth.soundboard.APPWIDGET_CONFIGURE");
		String[] columns = {SoundProvider._ID, SoundProvider.DESCRIPTION};
		Cursor cur = managedQuery(SoundProvider.TRACK_URI, columns, null, null, null);
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1,
				cur,
				new String[] {SoundProvider.DESCRIPTION},
				new int[] {android.R.id.text1}
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
			String[] columns = {SoundProvider.ACTION, SoundProvider.ASSET};
			Cursor cur = managedQuery(uri, columns, null, null, null);
			if (cur.moveToFirst()) {
				String action = cur.getString(cur.getColumnIndex(SoundProvider.ACTION));
				String asset = cur.getString(cur.getColumnIndex(SoundProvider.ASSET));
				Intent intent = new Intent(action);
				intent.setData(Uri.parse(asset));
				startService(intent);
			}
		}
	}

}
