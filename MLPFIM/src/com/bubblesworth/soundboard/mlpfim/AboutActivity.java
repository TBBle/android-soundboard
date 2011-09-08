/**
 * 
 */
package com.bubblesworth.soundboard.mlpfim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author paulh
 * 
 */
public class AboutActivity extends ListActivity implements
		MediaPlayer.OnPreparedListener {
	private static final String TAG = "AboutActivity";

	private static final String TEXT = "text";
	private static final String LINK = "link";

	List<Map<String, Object>> data;
	MediaPlayer bgPlayer;

	private class AboutViewBinder implements SimpleAdapter.ViewBinder {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.widget.SimpleAdapter.ViewBinder#setViewValue(android.view
		 * .View, java.lang.Object, java.lang.String)
		 */
		@Override
		public boolean setViewValue(View view, Object data,
				String textRepresentation) {
			if (!(view instanceof TextView)) {
				return false;
			}
			TextView tv = (TextView) view;
			if (data instanceof Spanned) {
				tv.setText((Spanned) data);
				return true;
			}
			if (data instanceof String) {
				if (((String) data).length() != 0) {
					tv.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
				} else {
					tv.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
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
		super.onCreate(savedInstanceState);
		String[] text = getResources().getStringArray(R.array.about_text);
		String[] links = getResources().getStringArray(R.array.about_links);
		assert text.length == links.length;
		data = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < text.length; ++i) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put(TEXT, Html.fromHtml(text[i]));
			map.put(LINK, links[i]);
			data.add(map);
		}

		SimpleAdapter adapter = new SimpleAdapter(this, data,
				R.layout.about_list_item, new String[] { TEXT, LINK },
				new int[] { R.id.aboutText, R.id.aboutText });
		adapter.setViewBinder(new AboutViewBinder());
		setListAdapter(adapter);
		onWindowFocusChanged(hasWindowFocus());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onWindowFocusChanged(boolean)
	 */
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus)
			startMusic();
		else
			stopMusic();
	}

	private void startMusic() {
		if (bgPlayer != null)
			return;
		bgPlayer = new MediaPlayer();
		bgPlayer.setOnPreparedListener(this);
		bgPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		bgPlayer.setLooping(true);
		AssetFileDescriptor bgMusic = getResources().openRawResourceFd(
				R.raw.pinkies_lie_superamazing);
		try {
			bgPlayer.setDataSource(bgMusic.getFileDescriptor(),
					bgMusic.getStartOffset(), bgMusic.getLength());
			bgPlayer.prepareAsync();
		} catch (IOException e) {
			Log.e(TAG, "Failed to setup background music", e);
			Toast.makeText(
					this,
					getResources().getText(
							R.string.toast_background_music_failed),
					Toast.LENGTH_SHORT).show();
			bgPlayer.release();
			bgPlayer = null;
		}
	}

	private void stopMusic() {
		if (bgPlayer == null)
			return;
		bgPlayer.stop();
		bgPlayer = null;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Map<String, Object> map = data.get(position);
		String link = (String) map.get(LINK);
		if (link.length() == 0) {
			return;
		}
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(link));
		try {
			startActivity(i);
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, "No activity for " + link, e);
			Toast.makeText(this,
					getResources().getText(R.string.toast_link_failed),
					Toast.LENGTH_SHORT).show();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media
	 * .MediaPlayer)
	 */
	@Override
	public void onPrepared(MediaPlayer mp) {
		mp.start();
	}

}
