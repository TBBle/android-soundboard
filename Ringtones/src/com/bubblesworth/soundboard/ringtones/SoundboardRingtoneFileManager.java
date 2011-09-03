package com.bubblesworth.soundboard.ringtones;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.bubblesworth.soundboard.SoundColumns;

public class SoundboardRingtoneFileManager {
	private static final String TAG = "SoundboardRingtoneFileManager";

	static final Uri CONTENT_URI = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
	private static final String RTAlbum = "com.bubblesworth.soundboard.ringtones";

	static Cursor query(Context context, String[] projection, String sortOrder) {
		ContentResolver contentResolver = context.getContentResolver();
		return contentResolver.query(CONTENT_URI, projection,
				MediaStore.Audio.Media.ALBUM + "=?", new String[] { RTAlbum },
				sortOrder);
	}

	static Uri getMediaUri(Context context, Uri trackUri) {
		ContentResolver contentResolver = context.getContentResolver();

		String[] columns = { SoundColumns.DESCRIPTION, SoundColumns.ASSET };
		Cursor cur = contentResolver.query(trackUri, columns, null, null, null);
		if (!cur.moveToFirst()) {
			Log.e(TAG,
					"Failed to retrieve details for trackURI: "
							+ trackUri.toString());
			return null;
		}

		String description = cur.getString(cur
				.getColumnIndex(SoundColumns.DESCRIPTION));
		String asset = cur.getString(cur.getColumnIndex(SoundColumns.ASSET));

		String trackHash;
		try {
			MessageDigest digester = MessageDigest.getInstance("SHA1");
			digester.update(trackUri.toString().getBytes());
			byte digest[] = digester.digest();
			StringBuilder hexString = new StringBuilder();
			for (int i = 0; i < digest.length; ++i) {
				hexString.append(Integer.toHexString(0xff & digest[i]));
			}
			trackHash = hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "No SHA1 algorithm", e);
			return null;
		}

		File trackFile = context.getFileStreamPath(trackUri.getAuthority()
				+ ":" + trackHash);
		// Log.d( TAG, "trackFile is " + trackFile.getAbsolutePath());
		Uri mediaUri;
		if (!trackFile.exists()) {
			InputStream assetStream;
			OutputStream trackStream;
			try {
				trackStream = context.openFileOutput(trackUri.getAuthority()
						+ ":" + trackHash, Context.MODE_WORLD_READABLE);
			} catch (FileNotFoundException e) {
				Log.e(TAG, "Failed to create " + trackFile.getAbsolutePath(), e);
				return null;
			}
			try {
				assetStream = contentResolver.openInputStream(Uri.parse(asset));
			} catch (FileNotFoundException e) {
				Log.e(TAG, "Failed to open input stream for " + asset, e);
				return null;
			}
			byte buffer[] = new byte[8192];
			int length;
			try {
				while (-1 != (length = assetStream.read(buffer))) {
					trackStream.write(buffer, 0, length);
				}
				assetStream.close();
				trackStream.close();
			} catch (IOException e) {
				Log.e(TAG, "Failed to copy asset", e);
				return null;
			}
			// Register our new track with the MediaStore
			ContentValues values = new ContentValues();
			values.put(MediaStore.Audio.Media.DATA, trackFile.getAbsolutePath());
			values.put(MediaStore.Audio.Media.TITLE, description);
			values.put(MediaStore.Audio.Media.ALBUM, RTAlbum);
			values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3");
			values.put(MediaStore.Audio.Media.IS_ALARM, true);
			values.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
			values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
			mediaUri = contentResolver.insert(CONTENT_URI, values);
		} else {
			// Query for the relevant track
			Cursor media = contentResolver.query(CONTENT_URI,
					new String[] { MediaStore.Audio.Media._ID },
					MediaStore.Audio.Media.DATA + "=?",
					new String[] { trackFile.getAbsolutePath() }, null);
			if (media.getCount() != 1 || !media.moveToFirst()) {
				Log.e(TAG,
						"Got unexpected number of results: " + media.getCount());
				return null;
			}
			mediaUri = ContentUris.withAppendedId(CONTENT_URI, media
					.getInt(media.getColumnIndex(MediaStore.Audio.Media._ID)));
		}
		// Log.d(TAG, "returning: " + mediaUri.toString());
		return mediaUri;
	}

	static void setMediaUriAsRingtone(Context context, Uri mediaUri, int type) {
		RingtoneManager.setActualDefaultRingtoneUri(context, type, mediaUri);
	}

	static void deleteMediaUri(Context context, Uri mediaUri) {
		ContentResolver contentResolver = context.getContentResolver();

		Cursor media = contentResolver.query(mediaUri,
				new String[] { MediaStore.Audio.Media.DATA },
				MediaStore.Audio.Media.ALBUM + "=?", new String[] { RTAlbum },
				null);
		if (!media.moveToFirst())
			return;
		File mediaFile = new File(media.getString(media
				.getColumnIndex(MediaStore.Audio.Media.DATA)));
		if (mediaFile.exists()) {
			context.deleteFile(mediaFile.getName());
		}
		contentResolver.delete(mediaUri, null, null);
	}

	static void validateMediaStore(Context context) {
		ContentResolver contentResolver = context.getContentResolver();

		Cursor media = contentResolver.query(CONTENT_URI, new String[] {
				MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA },
				MediaStore.Audio.Media.ALBUM + "=?", new String[] { RTAlbum },
				null);
		if (!media.moveToFirst())
			return;
		do {
			Uri mediaUri = ContentUris.withAppendedId(CONTENT_URI, media
					.getInt(media.getColumnIndex(MediaStore.Audio.Media._ID)));
			File mediaFile = new File(media.getString(media
					.getColumnIndex(MediaStore.Audio.Media.DATA)));
			if (!mediaFile.exists()) {
				Log.w(TAG, "Deleting Media at " + mediaUri.toString()
						+ " as file " + mediaFile.toString() + " is missing");
				if (contentResolver.delete(mediaUri, null, null) != 1)
					Log.e(TAG, "Failed to delete " + mediaUri.toString());
			}
		} while (media.moveToNext());
	}
}
