package com.bubblesworth.soundboard.mlpfim;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.Toast;

import com.bubblesworth.soundboard.SoundColumns;

/**
 * @author paulh
 * 
 */
public class SoundChooserActivity extends ExpandableListActivity {
	private static final String TAG = "SoundChooserActivity";

	private static final int DIALOG_WIDGET_HELP = 0;
	private static final int DIALOG_RINGTONES_HELP = 1;

	private boolean widgetConfig;

	private class MySimpleCursorTreeAdapter extends SimpleCursorTreeAdapter {

		public MySimpleCursorTreeAdapter(Context context, Cursor cursor,
				int groupLayout, String[] groupFrom, int[] groupTo,
				int childLayout, String[] childFrom, int[] childTo) {
			super(context, cursor, groupLayout, groupFrom, groupTo,
					childLayout, childFrom, childTo);
		}

		@Override
		protected Cursor getChildrenCursor(Cursor arg0) {
			String[] columns = { SoundColumns._ID, SoundColumns.DESCRIPTION,
					SoundColumns.ICON };
			return managedQuery(SoundProvider.TRACK_URI, columns,
					SoundColumns.CATEGORY_ID + "=?",
					new String[] { arg0.getString(arg0
							.getColumnIndex(SoundColumns._ID)) }, null);
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		widgetConfig = getIntent().getAction().equals(
				"com.bubblesworth.soundboard.APPWIDGET_CONFIGURE");
		String[] columns = { SoundColumns._ID, SoundColumns.DESCRIPTION,
				SoundColumns.ICON };
		Cursor cur = managedQuery(SoundProvider.CATEGORY_URI, columns, null,
				null, null);
		SimpleCursorTreeAdapter adapter = new MySimpleCursorTreeAdapter(this,
				cur, R.layout.icon_expandable_list_item, new String[] {
						SoundColumns.DESCRIPTION, SoundColumns.ICON },
				new int[] { R.id.listText, R.id.listIcon },
				R.layout.icon_list_item, new String[] {
						SoundColumns.DESCRIPTION, SoundColumns.ICON },
				new int[] { R.id.listText, R.id.listIcon });
		setListAdapter(adapter);
		if (widgetConfig) {
			setResult(RESULT_CANCELED);
		} else {
			registerForContextMenu(getExpandableListView());
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.ExpandableListActivity#onChildClick(android.widget.
	 * ExpandableListView, android.view.View, int, int, long)
	 */
	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		Uri uri = ContentUris.withAppendedId(SoundProvider.TRACK_URI, id);
		if (widgetConfig) {
			Intent resultValue = new Intent();
			resultValue.setData(uri);
			setResult(RESULT_OK, resultValue);
			finish();
		} else {
			String[] columns = { SoundColumns.ACTION, SoundColumns.ASSET };
			Cursor cur = managedQuery(uri, columns, null, null, null);
			if (cur.moveToFirst()) {
				String action = cur.getString(cur
						.getColumnIndex(SoundColumns.ACTION));
				String asset = cur.getString(cur
						.getColumnIndex(SoundColumns.ASSET));
				Intent intent = new Intent(action);
				intent.setData(Uri.parse(asset));
				startService(intent);
			}
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuAbout:
			startActivity(new Intent(this,
					com.bubblesworth.soundboard.mlpfim.AboutActivity.class));
			return true;
		case R.id.menuRingtones:
			return onMenuRingtonesClick();
		case R.id.menuWidget:
			return onMenuWidgetClick();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.ExpandableListActivity#onCreateContextMenu(android.view.
	 * ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.chooser_context_menu, menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item
				.getMenuInfo();
		Uri uri = ContentUris.withAppendedId(SoundProvider.TRACK_URI, info.id);
		int type = 0;
		switch (item.getItemId()) {
		case R.id.menuRingtone:
			type = RingtoneManager.TYPE_RINGTONE;
			break;
		case R.id.menuNotification:
			type = RingtoneManager.TYPE_NOTIFICATION;
			break;
		case R.id.menuAlarm:
			type = RingtoneManager.TYPE_ALARM;
			break;
		default:
			return super.onContextItemSelected(item);
		}
		Intent setter = new Intent(
				"com.bubblesworth.soundboard.ringtones.RINGTONE_SETTER");
		setter.setData(uri);
		setter.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, type);
		try {
			startActivity(setter);
		} catch (ActivityNotFoundException e) {
			installPackage("com.bubblesworth.soundboard.ringtones");
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
		case DIALOG_WIDGET_HELP:
			builder.setMessage(R.string.dialog_widget_help);
			return builder.create();
		case DIALOG_RINGTONES_HELP:
			CharSequence buttonLabels[] = getResources().getTextArray(
					R.array.dialog_ringtones_help_buttons);
			builder.setMessage(R.string.dialog_ringtones_help)
					.setPositiveButton(buttonLabels[0],
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									Intent intent = new Intent();
									intent.setClassName(
											"com.bubblesworth.soundboard.ringtones",
											"com.bubblesworth.soundboard.ringtones.SoundboardRingtoneManagerActivity");
									try {
										startActivity(intent);
									} catch (ActivityNotFoundException e) {
										Log.e(TAG,
												"No activity found for intent "
														+ intent.toString(), e);
										Toast.makeText(
												SoundChooserActivity.this,
												getResources()
														.getText(
																R.string.toast_no_manager),
												Toast.LENGTH_SHORT).show();
									}
									dialog.dismiss();
								}
							})
					.setNegativeButton(buttonLabels[1],
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									Intent intent = new Intent();
									intent.setAction(Settings.ACTION_SOUND_SETTINGS);
									try {
										startActivity(intent);
									} catch (ActivityNotFoundException e) {
										Log.e(TAG,
												"No activity found for intent "
														+ intent.toString(), e);
										Toast.makeText(
												SoundChooserActivity.this,
												getResources()
														.getText(
																R.string.toast_no_sound_settings),
												Toast.LENGTH_SHORT).show();
									}
									dialog.dismiss();
								}
							});
			return builder.create();
		}
		return super.onCreateDialog(id);
	}

	private boolean onMenuRingtonesClick() {
		boolean installed = requestInstallOfPackage("com.bubblesworth.soundboard.ringtones");
		if (installed) {
			showDialog(DIALOG_RINGTONES_HELP);
		}
		return true;
	}

	private boolean onMenuWidgetClick() {
		boolean installed = requestInstallOfPackage("com.bubblesworth.soundboard.widget");
		if (installed) {
			showDialog(DIALOG_WIDGET_HELP);
		}
		return true;
	}

	private boolean requestInstallOfPackage(String packageName) {
		PackageManager pm = getPackageManager();
		try {
			pm.getPackageInfo(packageName, 0);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			installPackage(packageName);
			return false;
		}
	}

	private void installPackage(String packageName) {
		String market = "market://details?id=" + packageName;
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(market));
		try {
			startActivity(i);
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, "No activity for " + market, e);
			Toast.makeText(SoundChooserActivity.this,
					getResources().getText(R.string.toast_no_market),
					Toast.LENGTH_SHORT).show();
		}
	}
}
