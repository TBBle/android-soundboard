/**
 * 
 */
package com.bubblesworth.soundboard.mlpfim;

import java.io.FileNotFoundException;
import java.io.IOException;

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

	private static final String SOUNDDIR = "pony sounds v4";

	private class SoundInfo {
		public int id;
		public String category;
		public String track;
		public String description;
		public int iconResource;
	};

	private SoundInfo sounds[];

	private static final int TRACKS = 1;
	private static final int TRACKS_ID = 2;
	private static final int ASSETS_ID = 4;
	private static final int ICONS_ID = 6;

	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		URI_MATCHER.addURI( AUTHORITY, "tracks", TRACKS );
		URI_MATCHER.addURI( AUTHORITY, "tracks/#", TRACKS_ID );
		URI_MATCHER.addURI( AUTHORITY, "assets/#", ASSETS_ID );
		URI_MATCHER.addURI( AUTHORITY, "icons/#", ICONS_ID );
	}

	public static final Uri CONTENT_URI = 
            Uri.parse("content://" + AUTHORITY);

	public static final Uri TRACK_URI =
            Uri.parse(CONTENT_URI+"/tracks");

	public static final Uri ASSET_URI =
            Uri.parse(CONTENT_URI+"/assets");

	public static final Uri ICON_URI =
            Uri.parse(CONTENT_URI+"/icons");

	// We reflect _ID and _COUNT from BaseColumns
	// We reflect DESCRIPTION, ACTION, ASSET, ICON from SoundColumns

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		int match = URI_MATCHER.match(uri);
		switch (match) {
		case TRACKS:
			return getContext().getResources().getString(R.string.mime_type_tracks);
		case TRACKS_ID:
			return getContext().getResources().getString(R.string.mime_type_track);
		case ASSETS_ID:
			return getContext().getResources().getString(R.string.mime_type_asset);
		case ICONS_ID:
			return getContext().getResources().getString(R.string.mime_type_icon);
		default:
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}
	
	// This is what Android allows in filenames that're referenced in R.drawable
	// Note that '.' is also not allowed, as Android's only allowing that for
	// the extension, it's stripped for things going into R.drawable
	private String idifyName( String name ) {
		return name.toLowerCase().replaceAll("[^a-z0-9_]", "");
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public boolean onCreate() {
		try {
			AssetManager assets = getContext().getAssets();
			Resources resources = getContext().getResources();
			int soundCount = 0;
			for (String category: assets.list(SOUNDDIR)) {
				soundCount+= assets.list(SOUNDDIR + "/" + category).length;
			}
			sounds = new SoundInfo[soundCount];
			soundCount = 0;
			for (String category: assets.list(SOUNDDIR)) {
				String categoryId = idifyName(category);
				int descriptionXmlResource = resources.getIdentifier( "xml/" + categoryId, null, "com.bubblesworth.soundboard.mlpfim" );
				XmlResourceParser xrp = null;
				String categoryName = category;
				try{
					xrp = resources.getXml( descriptionXmlResource );
					int eventType = xrp.getEventType();
					// Skip anything before the first tag
					while (eventType != XmlResourceParser.START_TAG) {
						eventType = xrp.next();
					}
					// Expect that we just hit a category tag
					xrp.require(XmlResourceParser.START_TAG, null, "category");
					// Check the category id
					String tagId = xrp.getAttributeValue(null, "id"); 
					if ( !tagId.equals( categoryId ) ) {
						throw new XmlPullParserException( "Got id " + tagId + " but expected id " + categoryId + " (" + xrp.getPositionDescription() + ")", xrp, null);
					}
					// Grab the description element
					categoryName = xrp.getAttributeValue(null, "description");
				} catch (NotFoundException e) {
					Log.e(TAG, "Failed to find category description resource", e);
				} catch (XmlPullParserException e) {
					Log.e(TAG, "Failed to parse category description", e);
					xrp.close();
					xrp = null;
				}
				int iconResource = resources.getIdentifier( "drawable/cat_" + categoryId, null, "com.bubblesworth.soundboard.mlpfim" );
				for (String track: assets.list(SOUNDDIR + "/" + category) ) {
					sounds[soundCount] = new SoundInfo();
					sounds[soundCount].id = soundCount;
					sounds[soundCount].category = category;
					sounds[soundCount].track = track;
					sounds[soundCount].iconResource = iconResource;

					int dotPos = track.lastIndexOf(".");
					String description = track.substring(0, dotPos);
					String trackId = idifyName(description);
					if (xrp != null) {
						try {
							xrp.nextTag();
							xrp.require(XmlResourceParser.START_TAG, null, "description" );
							String tagId = xrp.getAttributeValue(null, "id"); 
							if ( !tagId.equals( trackId ) ) {
								throw new XmlPullParserException( "Got id " + tagId + " but expected id " + trackId + " (" + xrp.getPositionDescription() + ")", xrp, null);
							}
							description = xrp.nextText();
							xrp.require(XmlResourceParser.END_TAG, null, "description");
						} catch (XmlPullParserException e) {
							Log.e(TAG, "Failed to parse sound description for trackId " + trackId, e);
							xrp.close();
							xrp = null;
						}
					}
					if (iconResource == 0)
						description = categoryName + " - " + description;
					sounds[soundCount].description = description;
					soundCount++;
				}
				if (xrp != null) {
					try {
						xrp.nextTag();
						xrp.require(XmlResourceParser.END_TAG, null, "category");
					} catch (XmlPullParserException e) {
						Log.e(TAG, "Didn't finish the category file", e);
					}
					xrp.close();
					xrp = null;
				}
			}
		} catch (IOException e) {
			Log.e(TAG, "onCreate", e);
			sounds = new SoundInfo[0];
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		//Log.d(TAG, "query(" + uri.toString() + ", " + projection.toString());
		int match = URI_MATCHER.match(uri);
		switch (match) {
		case TRACKS:
			return queryTracks(projection, selection, selectionArgs, sortOrder);
		case TRACKS_ID:
			return queryTrack(ContentUris.parseId(uri), projection);
		case ASSETS_ID:
			return null;
			//return queryAsset(ContentUris.parseId(uri));
		case ICONS_ID:
			return null;
			//return queryIcon(ContentUris.parseId(uri));
		default:
			return null;
		}
	}

	private Cursor queryTracks(String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		if (projection == null) {
			projection = new String[] { _ID, _COUNT, DESCRIPTION, ACTION, ASSET, ICON };
		}
		// TODO: Selection and sorting
		MatrixCursor result = new MatrixCursor(projection, sounds.length);
		for (SoundInfo sound:sounds) {
			MatrixCursor.RowBuilder row = result.newRow();
			populateRow(row, projection, sound);
		}
		if (result.getCount() == 0)
			return null;
		return result;
	}

	private Cursor queryTrack(long id, String[] projection) {
		if (id >= sounds.length || id < 0)
			return null;
		SoundInfo sound = sounds[(int)id];
		MatrixCursor result = new MatrixCursor(projection, 1);
		MatrixCursor.RowBuilder row = result.newRow();
		populateRow(row, projection, sound);
		return result;
	}

	private void populateRow(MatrixCursor.RowBuilder row, String[] columns, SoundInfo sound) {
		for (String column: columns) {
			if (column.equals(_ID))
				row.add(sound.id);
			else if (column.equals(DESCRIPTION))
				row.add(sound.description);
			else if (column.equals(ACTION))
				row.add("com.bubblesworth.soundboard.PLAY");
			else if (column.equals(ASSET))
				row.add(ContentUris.withAppendedId(ASSET_URI, (long)sound.id).toString());
			else if (column.equals(ICON))
				row.add(ContentUris.withAppendedId(ICON_URI, (long)sound.id).toString());
			else // TODO: _COUNT
				row.add(null);
		}
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#openAssetFile(android.net.Uri, java.lang.String)
	 */
	@Override
	public AssetFileDescriptor openAssetFile(Uri uri, String mode)
			throws FileNotFoundException {
		if (!mode.equals("r"))
			throw new FileNotFoundException();
		long id = ContentUris.parseId(uri);
		if (id >= sounds.length || id < 0)
			throw new FileNotFoundException();
		SoundInfo sound = sounds[(int)id];

		int match = URI_MATCHER.match(uri);
		switch (match) {
			case ASSETS_ID:
				String assetPath = SOUNDDIR + "/" + sound.category + "/" + sound.track;
				try {
					return getContext().getAssets().openFd(assetPath);
				} catch (IOException e) {
					Log.e(TAG, "openAssetFileDescriptor", e);
					throw new FileNotFoundException(e.getLocalizedMessage());
				}
			case ICONS_ID:
				if (sound.iconResource == 0)
					return getContext().getResources().openRawResourceFd(R.drawable.icon);
				return getContext().getResources().openRawResourceFd(sound.iconResource);
			default:
				throw new FileNotFoundException();
		}
	}

}
