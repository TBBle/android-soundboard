/**
 * 
 */
package com.bubblesworth.soundboard.mlpfim;

import java.io.IOException;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.bubblesworth.soundboard.CreditColumns;

/**
 * @author paulh
 * 
 */
public class AboutActivity extends ListActivity implements
		MediaPlayer.OnPreparedListener {
	private static final String TAG = "AboutActivity";

	MediaPlayer bgPlayer;

	private class AboutViewBinder implements SimpleCursorAdapter.ViewBinder {
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if (!(view instanceof TextView)) {
				return false;
			}
			TextView tv = (TextView) view;
			if (columnIndex == cursor
					.getColumnIndexOrThrow(CreditColumns.CREDIT_NAME)) {
				tv.setText(Html.fromHtml(cursor.getString(columnIndex)));
				return true;
			} else if (columnIndex == cursor
					.getColumnIndexOrThrow(CreditColumns.CREDIT_LINK)) {
				String uri = cursor.getString(columnIndex);
				if (uri.length() != 0) {
					tv.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
				} else {
					tv.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
				}
				return true;
			}
			return false;
		}
	}

	private GetCreditsCursor taskHolder;

	private class GetCreditsCursor extends AsyncTask<Void, Void, Cursor> {
		private ProgressDialog spinner;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (taskHolder != null)
				taskHolder.cancel(false);
			taskHolder = this;
			Resources resources = getResources();
			spinner = ProgressDialog.show(AboutActivity.this,
					resources.getString(R.string.dialog_loading_title),
					resources.getString(R.string.dialog_loading_message), true,
					false);
		}

		protected Cursor doInBackground(Void... voids) {
			String[] columns = { CreditColumns._ID, CreditColumns.CREDIT_NAME,
					CreditColumns.CREDIT_LINK };
			return managedQuery(SoundProvider.CREDIT_URI, columns, null, null,
					null);
		}

		protected void onPostExecute(Cursor result) {
			spinner.dismiss();
			SimpleCursorAdapter adapter = new SimpleCursorAdapter(
					AboutActivity.this, R.layout.about_list_item, result,
					new String[] { CreditColumns.CREDIT_NAME,
							CreditColumns.CREDIT_LINK }, new int[] {
							R.id.aboutText, R.id.aboutText });

			adapter.setViewBinder(new AboutViewBinder());
			setListAdapter(adapter);

			adapter.registerDataSetObserver(new DataSetObserver() {
				@Override
				public void onChanged() {
					new GetCreditsCursor().execute();
				}
			});
			taskHolder = null;
		}

		@Override
		protected void onCancelled() {
			spinner.dismiss();
			taskHolder = null;
			super.onCancelled();
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
		new GetCreditsCursor().execute();
		onWindowFocusChanged(hasWindowFocus());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onWindowFocusChanged(boolean)
	 */
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
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
		Cursor cursor = (Cursor) l.getItemAtPosition(position);
		String link = cursor.getString(cursor
				.getColumnIndexOrThrow(CreditColumns.CREDIT_LINK));
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
