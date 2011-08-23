/**
 * 
 */
package com.bubblesworth.soundboard.mlpfim;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import com.bubblesworth.soundboard.SoundColumns;

/**
 * @author tbble
 * 
 */
public class SoundProvider extends ContentProvider implements SoundColumns {
	private static final String TAG = "SoundProvider";

	private static final String AUTHORITY = "com.bubblesworth.soundboard.mlpfim.soundprovider";

	private class SoundInfo {
		public int id;
		public String track;
		public String description;
		public int iconResource;
	};

	private boolean loaded; // Was any data loaded?
	private Map<Integer, SoundInfo> sounds = null; // null if we need to try and
													// load data

	private static final int TRACKS = 1;
	private static final int TRACKS_ID = 2;
	private static final int ASSETS_ID = 4;
	private static final int ICONS_ID = 6;

	private static final UriMatcher URI_MATCHER = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		URI_MATCHER.addURI(AUTHORITY, "tracks", TRACKS);
		URI_MATCHER.addURI(AUTHORITY, "tracks/#", TRACKS_ID);
		URI_MATCHER.addURI(AUTHORITY, "assets/#", ASSETS_ID);
		URI_MATCHER.addURI(AUTHORITY, "icons/#", ICONS_ID);
	}

	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

	public static final Uri TRACK_URI = Uri.parse(CONTENT_URI + "/tracks");

	public static final Uri ASSET_URI = Uri.parse(CONTENT_URI + "/assets");

	public static final Uri ICON_URI = Uri.parse(CONTENT_URI + "/icons");

	// We reflect _ID and _COUNT from BaseColumns
	// We reflect DESCRIPTION, ACTION, ASSET, ICON from SoundColumns

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#delete(android.net.Uri,
	 * java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		int match = URI_MATCHER.match(uri);
		switch (match) {
		case TRACKS:
			return getContext().getResources().getString(
					R.string.mime_type_tracks);
		case TRACKS_ID:
			return getContext().getResources().getString(
					R.string.mime_type_track);
		case ASSETS_ID:
			return getContext().getResources().getString(
					R.string.mime_type_asset);
		case ICONS_ID:
			return getContext().getResources().getString(
					R.string.mime_type_icon);
		default:
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#insert(android.net.Uri,
	 * android.content.ContentValues)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public boolean onCreate() {
		loaded = false;
		return true;
	}

	private boolean loadData() {
		if (sounds != null)
			return loaded;

		sounds = new HashMap<Integer, SoundInfo>();
		loaded = loadSounds(R.xml.sounds);
		return loaded;
	}

	private boolean loadSounds(int soundsResource) {
		Resources resources = getContext().getResources();

		// Load our sounds.xml definition file.
		XmlResourceParser soundParser = null;
		try {
			soundParser = resources.getXml(soundsResource);
		} catch (NotFoundException e) {
			Log.e(TAG, "onCreate", e);
			return loaded;
		}

		try {
			int eventType = soundParser.getEventType();
			// Skip anything before the first tag
			while (eventType != XmlResourceParser.START_TAG) {
				eventType = soundParser.next();
			}
			// Expect that we just hit a "sounds" tag
			soundParser.require(XmlResourceParser.START_TAG, null, "sounds");
			String baseDir = soundParser.getAttributeValue(null, "src");
			eventType = soundParser.next();
			while (eventType != XmlResourceParser.END_TAG) {
				soundParser.require(XmlResourceParser.START_TAG, null,
						"category");
				String categoryId = soundParser.getAttributeValue(null, "id");
				int categoryValue = soundParser.getAttributeIntValue(null,
						"value", -1);
				assert categoryValue >= 0;
				soundParser.nextTag();

				try {
					loadCategory(baseDir, categoryId, categoryValue);
				} catch (Exception e) {
					// Log.e called later with this new exception doesn't
					// output the stacktrace from the chained exception.
					// So spam the logs a little...
					Log.e(TAG, "Error in loadCategory for " + categoryId, e);
					throw new XmlPullParserException(
							"Error in loadCategory for " + categoryId + " ( "
									+ categoryValue + " ): ("
									+ soundParser.getPositionDescription()
									+ ")", soundParser, e);
				}
				soundParser
						.require(XmlResourceParser.END_TAG, null, "category");
				eventType = soundParser.next();
			}
			soundParser.require(XmlResourceParser.END_TAG, null, "sounds");
			eventType = soundParser.next();
		} catch (XmlPullParserException e) {
			Log.e(TAG, "Failed to parse sounds description " + soundsResource,
					e);
			loaded = false;
			return loaded;
		} catch (IOException e) {
			Log.e(TAG, "Failed to parse sounds description " + soundsResource,
					e);
			loaded = false;
			return loaded;
		} finally {
			soundParser.close();
			soundParser = null;
		}
		loaded = true;
		return loaded;
	}

	// Bubble any exceptions to our caller...
	private void loadCategory(String baseDir, String categoryId,
			int categoryValue) throws Exception {
		AssetManager assets = getContext().getAssets();
		Resources resources = getContext().getResources();

		// Read the category XML file and ensure the sounds are present, then
		// add a SoundInfo object to sounds
		XmlResourceParser catParser = null;
		int catIconResource = resources.getIdentifier("drawable/cat_"
				+ categoryId, null, "com.bubblesworth.soundboard.mlpfim");
		int catXmlResource = resources.getIdentifier("xml/" + categoryId, null,
				"com.bubblesworth.soundboard.mlpfim");
		catParser = resources.getXml(catXmlResource);
		// Cleanup block for catParser
		try {
			int eventType = catParser.getEventType();
			while (eventType != XmlResourceParser.START_TAG) {
				eventType = catParser.next();
			}
			catParser.require(XmlResourceParser.START_TAG, null, "category");
			String tagId = catParser.getAttributeValue(null, "id");
			if (!tagId.equals(categoryId)) {
				throw new XmlPullParserException("Got id " + tagId
						+ " but expected id " + categoryId + " ("
						+ catParser.getPositionDescription() + ")", catParser,
						null);
			}
			String catDir = catParser.getAttributeValue(null, "src");
			HashSet<String> soundFiles = new HashSet<String>(
					Arrays.asList(assets.list(baseDir + "/" + catDir)));
			eventType = catParser.next();
			while (eventType != XmlResourceParser.END_TAG) {
				catParser.require(XmlResourceParser.START_TAG, null, "sound");
				String soundFile = catParser.getAttributeValue(null, "src");
				int soundValue = catParser.getAttributeIntValue(null, "value",
						-1);
				assert soundValue >= 0;
				assert soundValue < 1000;
				String soundDesc = catParser.nextText();

				if (!soundFiles.contains(soundFile + ".mp3")) {
					throw new FileNotFoundException(baseDir + "/" + catDir
							+ "/" + soundFile + ".mp3");
				}

				SoundInfo info = new SoundInfo();
				info.id = categoryValue * 1000 + soundValue;
				info.track = baseDir + "/" + catDir + "/" + soundFile + ".mp3";
				info.iconResource = catIconResource;
				if (catIconResource == 0)
					info.description = catDir + " - " + soundDesc;
				else
					info.description = soundDesc;
				Integer key = new Integer(info.id);
				assert !sounds.containsKey(key);
				sounds.put(key, info);

				catParser.require(XmlResourceParser.END_TAG, null, "sound");
				eventType = catParser.next();
			}
			catParser.require(XmlResourceParser.END_TAG, null, "category");
			catParser.next();
		} finally {
			catParser.close();
			catParser = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#query(android.net.Uri,
	 * java.lang.String[], java.lang.String, java.lang.String[],
	 * java.lang.String)
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		if (!loadData())
			return null;

		// Log.d(TAG, "query(" + uri.toString() + ", " + projection.toString());
		int match = URI_MATCHER.match(uri);
		switch (match) {
		case TRACKS:
			return queryTracks(projection, selection, selectionArgs, sortOrder);
		case TRACKS_ID:
			return queryTrack(ContentUris.parseId(uri), projection);
		case ASSETS_ID:
			return null;
			// return queryAsset(ContentUris.parseId(uri));
		case ICONS_ID:
			return null;
			// return queryIcon(ContentUris.parseId(uri));
		default:
			return null;
		}
	}

	private Cursor queryTracks(String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		if (projection == null) {
			projection = new String[] { _ID, _COUNT, DESCRIPTION, ACTION,
					ASSET, ICON };
		}
		// TODO: Selection and sorting
		MatrixCursor result = new MatrixCursor(projection, sounds.size());
		for (SoundInfo sound : sounds.values()) {
			MatrixCursor.RowBuilder row = result.newRow();
			populateRow(row, projection, sound);
		}
		if (result.getCount() == 0)
			return null;
		return result;
	}

	private Cursor queryTrack(long id, String[] projection) {
		assert id <= Integer.MAX_VALUE && id >= Integer.MIN_VALUE : id;
		Integer key = new Integer((int) id);
		if (!sounds.containsKey(key))
			return null;

		if (projection == null) {
			projection = new String[] { _ID, _COUNT, DESCRIPTION, ACTION,
					ASSET, ICON };
		}
		SoundInfo sound = sounds.get(key);
		MatrixCursor result = new MatrixCursor(projection, 1);
		MatrixCursor.RowBuilder row = result.newRow();
		populateRow(row, projection, sound);
		return result;
	}

	private void populateRow(MatrixCursor.RowBuilder row, String[] columns,
			SoundInfo sound) {
		for (String column : columns) {
			if (column.equals(_ID))
				row.add(sound.id);
			else if (column.equals(DESCRIPTION))
				row.add(sound.description);
			else if (column.equals(ACTION))
				row.add("com.bubblesworth.soundboard.PLAY");
			else if (column.equals(ASSET))
				row.add(ContentUris.withAppendedId(ASSET_URI, (long) sound.id)
						.toString());
			else if (column.equals(ICON))
				row.add(ContentUris.withAppendedId(ICON_URI, (long) sound.id)
						.toString());
			else
				// TODO: _COUNT
				row.add(null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#update(android.net.Uri,
	 * android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#openAssetFile(android.net.Uri,
	 * java.lang.String)
	 */
	@Override
	public AssetFileDescriptor openAssetFile(Uri uri, String mode)
			throws FileNotFoundException {
		if (!mode.equals("r"))
			throw new FileNotFoundException();
		if (!loadData())
			throw new FileNotFoundException();

		long id = ContentUris.parseId(uri);
		assert id <= Integer.MAX_VALUE && id >= Integer.MIN_VALUE : id;
		Integer key = new Integer((int) id);
		if (!sounds.containsKey(key))
			throw new FileNotFoundException();

		SoundInfo sound = sounds.get(key);

		int match = URI_MATCHER.match(uri);
		switch (match) {
		case ASSETS_ID:
			try {
				return getContext().getAssets().openFd(sound.track);
			} catch (IOException e) {
				Log.e(TAG, "openAssetFileDescriptor", e);
				throw new FileNotFoundException(e.getLocalizedMessage());
			}
		case ICONS_ID:
			if (sound.iconResource == 0)
				return getContext().getResources().openRawResourceFd(
						R.drawable.icon);
			return getContext().getResources().openRawResourceFd(
					sound.iconResource);
		default:
			throw new FileNotFoundException();
		}
	}

}
