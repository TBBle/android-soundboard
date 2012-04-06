package com.bubblesworth.soundboard.pack.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.bubblesworth.soundboard.pack.CategoryColumns;
import com.bubblesworth.soundboard.pack.CreditColumns;
import com.bubblesworth.soundboard.pack.SoundColumns;

abstract public class SoundPackProvider extends ContentProvider implements
		CategoryColumns, SoundColumns, CreditColumns {

	/* Methods for the subclasses to provide */
	abstract protected String getAuthority();

	abstract protected int getSoundsResource();

	abstract protected int getCreditsResource();

	private static final String TAG = "SoundPackProvider";

	/* Values uriMatcher may return */
	static protected final int SOUNDS = 1;
	static protected final int SOUNDS_ID = 2;
	static protected final int ASSETS_ID = 4;
	static protected final int ICONS_ID = 6;
	static protected final int CATEGORIES = 7;
	static protected final int CATEGORIES_ID = 8;
	static protected final int CREDITS = 9;
	static protected final int CREDITS_ID = 10;

	private UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	private Uri assetUri;
	private Uri iconUri;

	/* Define our databases */
	private static final String CATEGORY_TABLE = "Categories";
	// _ID
	// DESCRIPTION
	// ICON
	private static final String ICON_RESOURCE = "icon_resource";

	private static final String SOUND_TABLE = "Sounds";
	// _ID
	// CATEGORY_ID
	// DESCRIPTION
	// ACTION
	// ASSET
	private static final String ASSET_PATH = "asset_path";
	// ICON
	// ICON_RESOURCE shared with CATEGORY, as it's only internal

	private static final String CREDIT_TABLE = "Credits";

	// _ID
	// CREDIT_TYPE
	// CREDIT_NAME
	// CREDIT_LINK

	static class DatabaseHelper extends SQLiteOpenHelper {
		private static final String NAME = "soundpack.db";

		DatabaseHelper(Context context, int version) {
			super(context, NAME, null, version);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.database.sqlite.SQLiteOpenHelper#onCreate(android.database
		 * .sqlite.SQLiteDatabase)
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + CATEGORY_TABLE + " (" + _ID
					+ " INTEGER PRIMARY KEY, " + CategoryColumns.DESCRIPTION
					+ " TEXT, " + CategoryColumns.ICON + " TEXT, "
					+ ICON_RESOURCE + " TEXT" + ");");
			db.execSQL("CREATE TABLE " + SOUND_TABLE + " (" + _ID
					+ " INTEGER PRIMARY KEY, " + CATEGORY_ID + " INTEGER, "
					+ SoundColumns.DESCRIPTION + " TEXT, " + ACTION + " TEXT, "
					+ ASSET + " TEXT, " + ASSET_PATH + " TEXT, "
					+ SoundColumns.ICON + " TEXT, " + ICON_RESOURCE + " TEXT"
					+ ");");
			db.execSQL("CREATE TABLE " + CREDIT_TABLE + " (" + _ID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + CREDIT_TYPE
					+ " TEXT, " + CREDIT_NAME + " TEXT, " + CREDIT_LINK
					+ " TEXT" + ");");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database
		 * .sqlite.SQLiteDatabase, int, int)
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Always blow away and re-read the data...
			db.execSQL("DROP TABLE IF EXISTS " + CATEGORY_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + SOUND_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + CREDIT_TABLE);
			onCreate(db);
		}
	}

	private DatabaseHelper dbHelper;

	private boolean loaded; // Was any data loaded?

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#delete(android.net.Uri,
	 * java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		int match = uriMatcher.match(uri);
		switch (match) {
		case SOUNDS:
			return getContext().getResources().getString(
					R.string.mime_type_tracks);
		case SOUNDS_ID:
			return getContext().getResources().getString(
					R.string.mime_type_track);
		case ASSETS_ID:
			return getContext().getResources().getString(
					R.string.mime_type_asset);
		case ICONS_ID:
			return getContext().getResources().getString(
					R.string.mime_type_icon);
		case CATEGORIES:
			return getContext()
					.getResources()
					.getString(
							R.string.mime_type_categories);
		case CATEGORIES_ID:
			return getContext()
					.getResources()
					.getString(
							R.string.mime_type_category);
		case CREDITS:
			return getContext()
					.getResources()
					.getString(
							R.string.mime_type_credits);
		case CREDITS_ID:
			return getContext().getResources().getString(
					R.string.mime_type_credit);
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
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public synchronized boolean onCreate() {
		loaded = false;
		String authority = getAuthority();
		uriMatcher.addURI(authority, "sounds", SOUNDS);
		uriMatcher.addURI(authority, "sounds/#", SOUNDS_ID);
		uriMatcher.addURI(authority, "assets/#", ASSETS_ID);
		uriMatcher.addURI(authority, "icons/#", ICONS_ID);
		uriMatcher.addURI(authority, "categories", CATEGORIES);
		uriMatcher.addURI(authority, "categories/#", CATEGORIES_ID);
		uriMatcher.addURI(authority, "credits", CREDITS);
		uriMatcher.addURI(authority, "credits/#", CREDITS_ID);

		assetUri = Uri.parse("content://" + authority + "/assets");
		iconUri = Uri.parse("content://" + authority + "/icons");

		Context context = getContext();
		PackageManager packageManager = context.getPackageManager();
		try {
			dbHelper = new DatabaseHelper(
					context,
					packageManager.getPackageInfo(context.getPackageName(), 0).versionCode);
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "Failed to get version of package", e);
			return false;
		}
		return true;
	}

	private synchronized void loadData() {
		if (loaded)
			return;
		// Do we already have a populated database?
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(SOUND_TABLE);
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, null, null, null, null, null, null);
		if (c.getCount() != 0) {
			loaded = true;
			return;
		}

		db = dbHelper.getWritableDatabase();

		// Make sure there's not already sounds there...
		db.delete(CATEGORY_TABLE, null, null);
		db.delete(SOUND_TABLE, null, null);
		db.delete(CREDIT_TABLE, null, null);

		if (!loadSounds(getSoundsResource())) {
			db.delete(CATEGORY_TABLE, null, null);
			db.delete(SOUND_TABLE, null, null);
			return;
		}
		if (!loadCredits(getCreditsResource())) {
			db.delete(CATEGORY_TABLE, null, null);
			db.delete(SOUND_TABLE, null, null);
			db.delete(CREDIT_TABLE, null, null);
			return;
		}
		loaded = true;
	}

	private boolean loadCredits(int creditsResource) {
		if (creditsResource == 0)
			return true;
		Resources resources = getContext().getResources();
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		// Load our credits.xml definition file
		XmlResourceParser creditParser = null;
		try {
			creditParser = resources.getXml(creditsResource);
		} catch (NotFoundException e) {
			Log.e(TAG, "loadCredits", e);
			return false;
		}
		try {
			int eventType = creditParser.getEventType();
			// Skip anything before the first tag
			while (eventType != XmlResourceParser.START_TAG) {
				eventType = creditParser.next();
			}
			// Expect that we just hit a "credits" tag
			creditParser.require(XmlResourceParser.START_TAG, null, "credits");
			eventType = creditParser.next();
			while (eventType != XmlResourceParser.END_TAG) {
				creditParser.require(XmlResourceParser.START_TAG, null,
						"credit");
				String creditType = creditParser
						.getAttributeValue(null, "type");
				String creditUri = creditParser.getAttributeValue(null, "uri");
				String creditName = creditParser.nextText();
				creditParser.require(XmlResourceParser.END_TAG, null, "credit");
				eventType = creditParser.next();

				ContentValues creditValues = new ContentValues(3);
				creditValues.put(CREDIT_TYPE, creditType);
				creditValues.put(CREDIT_NAME, creditName);
				creditValues.put(CREDIT_LINK, creditUri);
				db.insertOrThrow(CREDIT_TABLE, null, creditValues);
			}
		} catch (XmlPullParserException e) {
			Log.e(TAG,
					"Failed to parse credits description " + creditsResource, e);
			return false;
		} catch (IOException e) {
			Log.e(TAG,
					"Failed to parse credits description " + creditsResource, e);
			return false;
		} finally {
			creditParser.close();
			creditParser = null;
		}
		return true;
	}

	private boolean loadSounds(int soundsResource) {
		if (soundsResource == 0)
			return true;
		Resources resources = getContext().getResources();

		// Load our sounds.xml definition file.
		XmlResourceParser soundParser = null;
		try {
			soundParser = resources.getXml(soundsResource);
		} catch (NotFoundException e) {
			Log.e(TAG, "loadSounds", e);
			return false;
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
				assert categoryValue > 0 && categoryValue < 1000;
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
			return false;
		} catch (IOException e) {
			Log.e(TAG, "Failed to parse sounds description " + soundsResource,
					e);
			return false;
		} finally {
			soundParser.close();
			soundParser = null;
		}
		return true;
	}

	// Bubble any exceptions to our caller...
	private void loadCategory(String baseDir, String categoryId,
			int categoryValue) throws Exception {
		AssetManager assets = getContext().getAssets();
		Resources resources = getContext().getResources();
		SQLiteDatabase db = dbHelper.getWritableDatabase();

		// Read the category XML file and ensure the sounds are present, then
		// add a SoundInfo object to sounds
		XmlResourceParser catParser = null;
		int catIconResource = resources.getIdentifier("drawable/cat_"
				+ categoryId, null, getContext().getPackageName());
		String catIconUri = ContentUris.withAppendedId(iconUri,
				(long) categoryValue).toString();
		int catXmlResource = resources.getIdentifier("xml/" + categoryId, null,
				getContext().getPackageName());
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
			catParser.require(XmlResourceParser.START_TAG, null, "description");
			String catDesc = catParser.nextText();
			catParser.require(XmlResourceParser.END_TAG, null, "description");
			eventType = catParser.next();

			ContentValues categoryValues = new ContentValues(4);
			categoryValues.put(_ID, categoryValue);
			categoryValues.put(CategoryColumns.DESCRIPTION, catDesc);
			categoryValues.put(CategoryColumns.ICON, catIconUri);
			categoryValues.put(ICON_RESOURCE, catIconResource);
			long categoryDbId = db.insertOrThrow(CATEGORY_TABLE, null,
					categoryValues);
			assert categoryDbId == categoryValue;

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

				ContentValues soundValues = new ContentValues();
				long soundFullValue = categoryValue * 1000 + soundValue;
				soundValues.put(_ID, soundFullValue);
				soundValues.put(CATEGORY_ID, categoryValue);
				if (catIconResource == 0)
					soundValues.put(SoundColumns.DESCRIPTION, catDesc + " - "
							+ soundDesc);
				else
					soundValues.put(SoundColumns.DESCRIPTION, soundDesc);
				soundValues.put(ACTION, "com.bubblesworth.soundboard.PLAY");
				soundValues.put(ASSET,
						ContentUris.withAppendedId(assetUri, soundFullValue)
								.toString());
				soundValues.put(ASSET_PATH, baseDir + "/" + catDir + "/"
						+ soundFile + ".mp3");
				soundValues.put(SoundColumns.ICON, catIconUri);
				soundValues.put(ICON_RESOURCE, 0);
				long soundDbId = db.insertOrThrow(SOUND_TABLE, null,
						soundValues);
				assert soundDbId == soundFullValue;

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
		loadData();
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		int match = uriMatcher.match(uri);
		switch (match) {
		case SOUNDS_ID:
			qb.appendWhere(_ID + "=" + ContentUris.parseId(uri));
			// Fall-through
		case SOUNDS:
			qb.setTables(SOUND_TABLE);
			if (TextUtils.isEmpty(sortOrder)) {
				sortOrder = CATEGORY_ID + " ASC, " + SoundColumns.DESCRIPTION
						+ " ASC";
			}
			if (projection == null) {
				// Hide the ICON_RESOURCE and ASSET_RESOURCE columns
				projection = new String[] { _ID, CATEGORY_ID,
						SoundColumns.DESCRIPTION, ACTION, ASSET,
						SoundColumns.ICON };
			}
			break;
		case CATEGORIES_ID:
			qb.appendWhere(_ID + "=" + ContentUris.parseId(uri));
			// Fall-through
		case CATEGORIES:
			qb.setTables(CATEGORY_TABLE);
			if (TextUtils.isEmpty(sortOrder)) {
				sortOrder = CategoryColumns.DESCRIPTION + " ASC";
			}
			if (projection == null) {
				// Hide the ICON_RESOURCE column
				projection = new String[] { _ID, CategoryColumns.DESCRIPTION,
						CategoryColumns.ICON };
			}
			break;
		case CREDITS_ID:
			qb.appendWhere(_ID + "=" + ContentUris.parseId(uri));
			// Fall-through
		case CREDITS:
			qb.setTables(CREDIT_TABLE);
			if (TextUtils.isEmpty(sortOrder)) {
				sortOrder = CREDIT_TYPE + " ASC, " + CREDIT_NAME + " ASC";
			}
			break;
		case ASSETS_ID:
		case ICONS_ID:
		default:
			return null;
		}
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
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
		loadData();
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		long id = ContentUris.parseId(uri);

		// See loadSounds and loadCategory
		if (id < 1000) {
			qb.setTables(CATEGORY_TABLE);
		} else {
			qb.setTables(SOUND_TABLE);
		}

		qb.appendWhere(_ID + "=" + id);

		SQLiteDatabase db = dbHelper.getReadableDatabase();

		String column;
		Cursor c;
		int match = uriMatcher.match(uri);
		switch (match) {
		case ASSETS_ID:
			column = ASSET_PATH;
			c = qb.query(db, new String[] { column }, null, null, null, null,
					null);
			if (!c.moveToFirst())
				throw new FileNotFoundException();
			String path = c.getString(c.getColumnIndexOrThrow(column));
			try {
				return getContext().getAssets().openFd(path);
			} catch (IOException e) {
				Log.e(TAG, "openAssetFileDescriptor", e);
				throw new FileNotFoundException(e.getLocalizedMessage());
			}
		case ICONS_ID:
			column = ICON_RESOURCE;
			c = qb.query(db, new String[] { column }, null, null, null, null,
					null);
			if (!c.moveToFirst())
				throw new FileNotFoundException();
			int iconResId = c.getInt(c.getColumnIndexOrThrow(column));
			return getContext().getResources().openRawResourceFd(iconResId);
		default:
			throw new FileNotFoundException();
		}
	}

}
