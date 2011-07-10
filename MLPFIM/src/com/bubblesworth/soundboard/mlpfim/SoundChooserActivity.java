package com.bubblesworth.soundboard.mlpfim;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
//import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class SoundChooserActivity extends ListActivity {
	//private static final String TAG = "SoundChooserActivity";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.main);
		//ListView list = (ListView)findViewById(R.id.soundList);
		String[] columns = {SoundProvider._ID, SoundProvider.DESCRIPTION};
		Cursor cur = managedQuery(SoundProvider.TRACK_URI, columns, null, null, null);
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1,
				cur,
				new String[] {SoundProvider.DESCRIPTION},
				new int[] {android.R.id.text1}
				);
		setListAdapter(adapter);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		//Log.d(TAG, "Clicked id " + id + " for intent " + getIntent().toString());
		Intent intent = new Intent(SoundChooserActivity.this, SoundboardService.class);
		String[] columns = {SoundProvider.ASSET};
		Cursor cur = managedQuery(ContentUris.withAppendedId(SoundProvider.TRACK_URI, id), columns, null, null, null);
		if (cur.moveToFirst()) {
			String asset = cur.getString(cur.getColumnIndex(SoundProvider.ASSET));
			intent.setData(Uri.parse(asset));
			startService(intent);
		}
	}
}