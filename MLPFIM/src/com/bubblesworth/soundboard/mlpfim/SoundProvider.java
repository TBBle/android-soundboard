/**
 * 
 */
package com.bubblesworth.soundboard.mlpfim;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.bubblesworth.soundboard.CategoryColumns;
import com.bubblesworth.soundboard.CreditColumns;
import com.bubblesworth.soundboard.SoundColumns;

/**
 * @author tbble
 * 
 */
public class SoundProvider extends ContentProvider implements CategoryColumns,
		SoundColumns, CreditColumns {
	private static final String TAG = "SoundProvider";

	private static final String AUTHORITY = "com.bubblesworth.soundboard.mlpfim.soundprovider";

	/* Define our databases */
	private static final String SOURCE_TABLE = "Sources";
	// _ID
	private static final String PACKAGE_NAME = "package_name";
	private static final String PACKAGE_VERSION = "package_version";
	private static final String PACKAGE_INSTALLED = "package_installed";

	private static final String CATEGORY_TABLE = "Categories";
	// _ID
	private static final String SOURCE_ID = "source_id";
	// CategoryColumns.DESCRIPTION
	// CategoryColumns.ICON

	private static final String SOUND_TABLE = "Sounds";
	// _ID
	// CATEGORY_ID
	// SoundColumns.DESCRIPTION
	// ACTION
	// ASSET
	// SoundColumns.ICON

	private static final String CREDIT_TABLE = "Credits";

	// _ID
	// SOURCE_ID
	// CREDIT_TYPE
	// CREDIT_NAME
	// CREDIT_LINK
	// CREDIT_ORDER

	static class DatabaseHelper extends SQLiteOpenHelper {
		// private static final String TAG = "DatabaseHelper";
		private static final String NAME = "sounds.db";
		private static final int VERSION = 2;

		DatabaseHelper(Context context) {
			super(context, NAME, null, VERSION);
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
			db.execSQL("CREATE TABLE " + SOURCE_TABLE + " (" + _ID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + PACKAGE_NAME
					+ " TEXT UNIQUE, " + PACKAGE_VERSION + " INTEGER, "
					+ PACKAGE_INSTALLED + " INTEGER" + ");");
			db.execSQL("CREATE TABLE " + CATEGORY_TABLE + " (" + _ID
					+ " INTEGER PRIMARY KEY, " + SOURCE_ID + " INTEGER, "
					+ CategoryColumns.DESCRIPTION + " TEXT, "
					+ CategoryColumns.ICON + " TEXT" + ");");
			db.execSQL("CREATE TABLE " + SOUND_TABLE + " (" + _ID
					+ " INTEGER PRIMARY KEY, " + CATEGORY_ID + " INTEGER, "
					+ SoundColumns.DESCRIPTION + " TEXT, " + ACTION + " TEXT, "
					+ ASSET + " TEXT, " + SoundColumns.ICON + " TEXT" + ");");
			db.execSQL("CREATE TABLE " + CREDIT_TABLE + " (" + _ID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + SOURCE_ID
					+ " INTEGER, " + CREDIT_TYPE + " TEXT, " + CREDIT_NAME
					+ " TEXT, " + CREDIT_LINK + " TEXT," + CREDIT_ORDER
					+ " INTEGER" + ");");
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
			// Version 2: Clean up CREDIT_TABLE table entries with a dangling
			// SOURCE_ID
			if (oldVersion < 2) {
				SQLiteQueryBuilder creditsQuery = new SQLiteQueryBuilder();
				creditsQuery.setTables(CREDIT_TABLE);
				creditsQuery.setDistinct(true);
				Cursor creditsSources = creditsQuery.query(db,
						new String[] { SOURCE_ID }, null, null, null, null,
						null);
				if (creditsSources.moveToFirst()) {
					SQLiteQueryBuilder sourcesQuery = new SQLiteQueryBuilder();
					sourcesQuery.setTables(SOURCE_TABLE);
					sourcesQuery.appendWhere(_ID + "=?");
					do {
						int sourceId = creditsSources.getInt(creditsSources
								.getColumnIndexOrThrow(SOURCE_ID));
						Cursor source = sourcesQuery.query(db,
								new String[] { _ID }, null, new String[] { ""
										+ sourceId }, null, null, null);
						if (source.getCount() != 0)
							continue;
						db.delete(CREDIT_TABLE, SOURCE_ID + " = ?",
								new String[] { "" + sourceId });
					} while (creditsSources.moveToNext());
				}
			}
		}
	}

	private DatabaseHelper dbHelper;

	private class PackChangeReceiver extends BroadcastReceiver {
		private static final String TAG = "PackChangeReceiver";

		private HashSet<String> knownPackages = new HashSet<String>();

		public void register(Context context) {
			IntentFilter packChangeFilter = new IntentFilter();
			packChangeFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
			packChangeFilter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
			packChangeFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
			packChangeFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
			// Found via
			// http://stackoverflow.com/questions/3838794/android-response-from-market-activity/3872814#3872814
			packChangeFilter.addDataScheme("package");
			context.registerReceiver(this, packChangeFilter);
		}

		public void clearPackages() {
			knownPackages.clear();
		}

		public void registerPackage(String packName) {
			knownPackages.add(packName);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			// Also via
			// http://stackoverflow.com/questions/3838794/android-response-from-market-activity/3872814#3872814
			String packName = intent.getData().getSchemeSpecificPart();
			String action = intent.getAction();
			boolean refresh = false;
			// Did we know about this?
			if (!action.equals(Intent.ACTION_PACKAGE_ADDED)) {
				refresh = knownPackages.contains(packName);
			}
			if (!refresh
					&& (action.equals(Intent.ACTION_PACKAGE_ADDED) || action
							.equals(Intent.ACTION_PACKAGE_REPLACED))) {
				// If it's a new package, or might have grown a content
				// provider, we need to know.
				PackageManager packageManager = context.getPackageManager();
				PackageInfo packInfo;
				try {
					packInfo = packageManager.getPackageInfo(packName,
							PackageManager.GET_PROVIDERS
									+ PackageManager.GET_META_DATA);
				} catch (NameNotFoundException e) {
					Log.e(TAG, "Added/Replaced package " + packName
							+ " isn't found in PackageManager", e);
					return;
				}
				if (packInfo.providers == null)
					return;
				for (ProviderInfo provider : packInfo.providers) {
					if (provider.metaData == null)
						continue;
					String categories = provider.metaData
							.getString("com.bubblesworth.soundboard.mlpfim.packs.categories");
					String sounds = provider.metaData
							.getString("com.bubblesworth.soundboard.mlpfim.packs.sounds");
					String credits = provider.metaData
							.getString("com.bubblesworth.soundboard.mlpfim.packs.credits");
					// Not one of our providers...
					if (categories == null || sounds == null || credits == null)
						continue;
					refresh = true;
					break;
				}
			}
			if (refresh) {
				synchronized (this) {
					SoundProvider.this.loaded = false;
					context.getContentResolver().notifyChange(CATEGORY_URI,
							null);
					context.getContentResolver().notifyChange(TRACK_URI, null);
					context.getContentResolver().notifyChange(CREDIT_URI, null);
				}
			}
		}
	}

	private PackChangeReceiver packChangeReceiver;

	private boolean loaded; // Was any data loaded?

	private static final int TRACKS = 1;
	private static final int TRACKS_ID = 2;
	private static final int ASSETS_ID = 4;
	private static final int ICONS_ID = 6;
	private static final int CATEGORIES = 7;
	private static final int CATEGORIES_ID = 8;
	private static final int CREDITS = 9;
	private static final int CREDITS_ID = 10;

	private static final UriMatcher URI_MATCHER = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		URI_MATCHER.addURI(AUTHORITY, "tracks", TRACKS);
		URI_MATCHER.addURI(AUTHORITY, "tracks/#", TRACKS_ID);
		URI_MATCHER.addURI(AUTHORITY, "assets/#", ASSETS_ID);
		URI_MATCHER.addURI(AUTHORITY, "icons/#", ICONS_ID);
		URI_MATCHER.addURI(AUTHORITY, "categories", CATEGORIES);
		URI_MATCHER.addURI(AUTHORITY, "categories/#", CATEGORIES_ID);
		URI_MATCHER.addURI(AUTHORITY, "credits", CREDITS);
		URI_MATCHER.addURI(AUTHORITY, "credits/#", CREDITS_ID);
	}

	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

	public static final Uri TRACK_URI = Uri.parse(CONTENT_URI + "/tracks");

	public static final Uri ASSET_URI = Uri.parse(CONTENT_URI + "/assets");

	public static final Uri ICON_URI = Uri.parse(CONTENT_URI + "/icons");

	public static final Uri CATEGORY_URI = Uri.parse(CONTENT_URI
			+ "/categories");

	public static final Uri CREDIT_URI = Uri.parse(CONTENT_URI + "/credits");

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#delete(android.net.Uri,
	 * java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO: Favourites support
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
					com.bubblesworth.soundboard.R.string.mime_type_tracks);
		case TRACKS_ID:
			return getContext().getResources().getString(
					com.bubblesworth.soundboard.R.string.mime_type_track);
		case ASSETS_ID:
			return getContext()
					.getResources()
					.getString(
							com.bubblesworth.soundboard.mlpfim.R.string.mime_type_asset);
		case ICONS_ID:
			return getContext().getResources().getString(
					com.bubblesworth.soundboard.mlpfim.R.string.mime_type_icon);
		case CATEGORIES:
			return getContext().getResources().getString(
					com.bubblesworth.soundboard.R.string.mime_type_categories);
		case CATEGORIES_ID:
			return getContext().getResources().getString(
					com.bubblesworth.soundboard.R.string.mime_type_category);
		case CREDITS:
			return getContext().getResources().getString(
					com.bubblesworth.soundboard.R.string.mime_type_credits);
		case CREDITS_ID:
			return getContext().getResources().getString(
					com.bubblesworth.soundboard.R.string.mime_type_credit);
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
		// TODO: Favourites support
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
		Context context = getContext();
		packChangeReceiver = new PackChangeReceiver();
		packChangeReceiver.register(context);
		dbHelper = new DatabaseHelper(context);
		return true;
	}

	private final class SoundProviderInfo {
		String name;
		int version;
		Uri categories;
		Uri sounds;
		Uri credits;
	}

	private ArrayList<SoundProviderInfo> findProviders() {
		ArrayList<SoundProviderInfo> result = new ArrayList<SoundProviderInfo>();
		Context context = getContext();
		PackageManager packageManager = context.getPackageManager();
		List<ProviderInfo> providers = packageManager.queryContentProviders(
				null, 0, PackageManager.GET_META_DATA);
		for (ProviderInfo provider : providers) {
			if (provider.metaData == null)
				continue;
			String categories = provider.metaData
					.getString("com.bubblesworth.soundboard.mlpfim.packs.categories");
			String sounds = provider.metaData
					.getString("com.bubblesworth.soundboard.mlpfim.packs.sounds");
			String credits = provider.metaData
					.getString("com.bubblesworth.soundboard.mlpfim.packs.credits");
			// Not one of our providers...
			if (categories == null || sounds == null || credits == null)
				continue;
			String name = provider.applicationInfo.packageName;
			int version;
			try {
				version = packageManager.getPackageInfo(name, 0).versionCode;
			} catch (PackageManager.NameNotFoundException e) {
				Log.e(TAG,
						"Package responded to activity query but not found by package manager",
						e);
				continue;
			}
			SoundProviderInfo newProvider = new SoundProviderInfo();
			newProvider.name = name;
			newProvider.version = version;
			newProvider.categories = Uri.parse(categories);
			newProvider.sounds = Uri.parse(sounds);
			newProvider.credits = Uri.parse(credits);
			result.add(newProvider);
		}
		return result;
	}

	private synchronized void loadData() {
		if (loaded)
			return;
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		ContentValues clearPackage = new ContentValues();
		clearPackage.put(PACKAGE_INSTALLED, false);
		db.update(SOURCE_TABLE, clearPackage, null, null);
		packChangeReceiver.clearPackages();

		ContentValues setPackage = new ContentValues();
		setPackage.put(PACKAGE_INSTALLED, true);

		ArrayList<SoundProviderInfo> newProviders = new ArrayList<SoundProviderInfo>();

		SQLiteQueryBuilder sourceQuery = new SQLiteQueryBuilder();
		sourceQuery.setTables(SOURCE_TABLE);
		for (SoundProviderInfo provider : findProviders()) {
			Cursor sourceCursor = sourceQuery.query(db, null, PACKAGE_NAME
					+ "=?", new String[] { provider.name }, null, null, null);
			assert sourceCursor.getCount() == 0 || sourceCursor.getCount() == 1;
			if (sourceCursor.getCount() == 0) {
				// New pack
				newProviders.add(provider);
			} else {
				sourceCursor.moveToFirst();
				int sourceInstalled = sourceCursor.getInt(sourceCursor
						.getColumnIndexOrThrow(PACKAGE_INSTALLED));
				// Already processed... Duplicate activity result?
				if (sourceInstalled == 1)
					continue;
				long sourceId = sourceCursor.getLong(sourceCursor
						.getColumnIndexOrThrow(_ID));
				int sourceVersion = sourceCursor.getInt(sourceCursor
						.getColumnIndexOrThrow(PACKAGE_VERSION));
				// Version is what we already know
				if (sourceVersion == provider.version) {
					db.update(SOURCE_TABLE, setPackage, _ID + " =?",
							new String[] { "" + sourceId });
					packChangeReceiver.registerPackage(provider.name);
					continue;
				}
				// Known source, but new version. Easiest thing is to blow it
				// away and reimport it.
				deletePack(db, sourceId);
				newProviders.add(provider);
			}
		}

		Cursor goneProviders = sourceQuery.query(db, new String[] { _ID },
				PACKAGE_INSTALLED + "=0", null, null, null, null);
		if (goneProviders.moveToFirst()) {
			do {
				deletePack(db, goneProviders.getLong(goneProviders
						.getColumnIndexOrThrow(_ID)));
			} while (goneProviders.moveToNext());
		}
		for (SoundProviderInfo newProvider : newProviders) {
			importPack(db, newProvider.name, newProvider.version,
					newProvider.categories, newProvider.sounds,
					newProvider.credits);
			packChangeReceiver.registerPackage(newProvider.name);
		}

		// dumpDatabases();

		loaded = true;
		return;
	}

	@SuppressWarnings("unused")
	private void dumpDatabases() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(SOURCE_TABLE);
		Cursor source = qb.query(db, null, null, null, null, null, null);
		Log.d(TAG, "dump " + SOURCE_TABLE);
		if (source.moveToFirst()) {
			do {
				Log.d(TAG,
						"|"
								+ source.getLong(source
										.getColumnIndexOrThrow(_ID))
								+ "|"
								+ source.getString(source
										.getColumnIndexOrThrow(PACKAGE_NAME))
								+ "|"
								+ source.getInt(source
										.getColumnIndexOrThrow(PACKAGE_VERSION))
								+ "|"
								+ source.getInt(source
										.getColumnIndexOrThrow(PACKAGE_INSTALLED)));
			} while (source.moveToNext());
		}
		source.close();

		qb.setTables(CATEGORY_TABLE);
		Cursor category = qb.query(db, null, null, null, null, null, null);
		Log.d(TAG, "dump " + CATEGORY_TABLE);
		if (category.moveToFirst()) {
			do {
				Log.d(TAG,
						"|"
								+ category.getLong(category
										.getColumnIndexOrThrow(_ID))
								+ "|"
								+ category.getInt(category
										.getColumnIndexOrThrow(SOURCE_ID))
								+ "|"
								+ category.getString(category
										.getColumnIndexOrThrow(CategoryColumns.DESCRIPTION))
				// + "|" +
				// category.getString(category.getColumnIndexOrThrow(CategoryColumns.ICON))
				);
			} while (category.moveToNext());
		}
		category.close();

		qb.setTables(SOUND_TABLE);
		Cursor sound = qb.query(db, null, null, null, null, null, null);
		Log.d(TAG, "dump " + SOUND_TABLE + ": " + sound.getCount() + " rows");
		sound.close();

		qb.setTables(CREDIT_TABLE);
		Cursor credit = qb.query(db, null, null, null, null, null, null);
		Log.d(TAG, "dump " + CREDIT_TABLE);
		if (credit.moveToFirst()) {
			do {
				Log.d(TAG,
						"|"
								+ credit.getLong(credit
										.getColumnIndexOrThrow(_ID))
								+ "|"
								+ credit.getInt(credit
										.getColumnIndexOrThrow(SOURCE_ID))
								+ "|"
								+ credit.getString(credit
										.getColumnIndexOrThrow(CREDIT_TYPE))
								+ "|"
								+ credit.getString(credit
										.getColumnIndexOrThrow(CREDIT_NAME))
								+ "|"
								+ credit.getString(credit
										.getColumnIndexOrThrow(CREDIT_LINK))
								+ "|"
								+ credit.getInt(credit
										.getColumnIndexOrThrow(CREDIT_ORDER)));
			} while (credit.moveToNext());
		}
		credit.close();

		Log.d(TAG, "dump ends");
	}

	private void importPack(SQLiteDatabase db, String name, int version,
			Uri categoriesUri, Uri soundsUri, Uri creditsUri) {
		/*
		 * Theory: Query the content provider at categoriesUri, make sure
		 * there's no categories overlap. Then add the categories to my known
		 * list, and the sounds to my known list. And record the pack itself.
		 */
		ContentValues sourceValues = new ContentValues(3);
		sourceValues.put(PACKAGE_NAME, name);
		sourceValues.put(PACKAGE_VERSION, version);
		sourceValues.put(PACKAGE_INSTALLED, 1);

		long sourceId = db.insertOrThrow(SOURCE_TABLE, null, sourceValues);
		Cursor sourceCategories = getContext()
				.getContentResolver()
				.query(categoriesUri,
						new String[] {
								com.bubblesworth.soundboard.pack.CategoryColumns._ID,
								com.bubblesworth.soundboard.pack.CategoryColumns.DESCRIPTION,
								com.bubblesworth.soundboard.pack.CategoryColumns.ICON },
						null, null, null);
		Cursor sourceSounds = getContext().getContentResolver().query(
				soundsUri, null, null, null, null);
		Cursor sourceCredits = getContext()
				.getContentResolver()
				.query(creditsUri,
						new String[] {
								com.bubblesworth.soundboard.pack.CreditColumns.CREDIT_TYPE,
								com.bubblesworth.soundboard.pack.CreditColumns.CREDIT_NAME,
								com.bubblesworth.soundboard.pack.CreditColumns.CREDIT_LINK },
						null, null, null);

		try {
			if (sourceCategories.moveToFirst() && sourceSounds.moveToFirst()) {
				do {
					ContentValues categoryValues = new ContentValues(4);
					long categoryId = sourceCategories
							.getLong(sourceCategories
									.getColumnIndexOrThrow(com.bubblesworth.soundboard.pack.CategoryColumns._ID));
					categoryValues.put(_ID, categoryId);
					categoryValues.put(SOURCE_ID, sourceId);
					categoryValues
							.put(CategoryColumns.DESCRIPTION,
									sourceCategories.getString(sourceCategories
											.getColumnIndexOrThrow(com.bubblesworth.soundboard.pack.CategoryColumns.DESCRIPTION)));
					categoryValues
							.put(CategoryColumns.ICON,
									sourceCategories.getString(sourceCategories
											.getColumnIndexOrThrow(com.bubblesworth.soundboard.pack.CategoryColumns.ICON)));
					long storedCategoryId = db.insertOrThrow(CATEGORY_TABLE,
							null, categoryValues);
					assert storedCategoryId < 1000;
					assert storedCategoryId == categoryId;
				} while (sourceCategories.moveToNext());
				do {
					ContentValues soundValues = new ContentValues(6);
					long soundId = sourceSounds
							.getLong(sourceSounds
									.getColumnIndexOrThrow(com.bubblesworth.soundboard.pack.SoundColumns._ID));
					soundValues.put(_ID, soundId);
					soundValues
							.put(CATEGORY_ID,
									sourceSounds.getString(sourceSounds
											.getColumnIndexOrThrow(com.bubblesworth.soundboard.pack.SoundColumns.CATEGORY_ID)));
					soundValues
							.put(SoundColumns.DESCRIPTION,
									sourceSounds.getString(sourceSounds
											.getColumnIndexOrThrow(com.bubblesworth.soundboard.pack.SoundColumns.DESCRIPTION)));
					soundValues
							.put(ACTION,
									sourceSounds.getString(sourceSounds
											.getColumnIndexOrThrow(com.bubblesworth.soundboard.pack.SoundColumns.ACTION)));
					soundValues
							.put(ASSET,
									sourceSounds.getString(sourceSounds
											.getColumnIndexOrThrow(com.bubblesworth.soundboard.pack.SoundColumns.ASSET)));
					soundValues
							.put(SoundColumns.ICON,
									sourceSounds.getString(sourceSounds
											.getColumnIndexOrThrow(com.bubblesworth.soundboard.pack.SoundColumns.ICON)));
					long storedSoundId = db.insertOrThrow(SOUND_TABLE, null,
							soundValues);
					assert storedSoundId == soundId;
				} while (sourceSounds.moveToNext());
			}
			// This is a little odd... Pack with credits but no sounds?
			// The actual Soundboard itself does this. Hopefully no one else
			// does...
			if (sourceCredits.moveToFirst()) {
				do {
					ContentValues creditValues = new ContentValues(5);
					creditValues.put(SOURCE_ID, sourceId);
					creditValues
							.put(CREDIT_NAME,
									sourceCredits.getString(sourceCredits
											.getColumnIndexOrThrow(com.bubblesworth.soundboard.pack.CreditColumns.CREDIT_NAME)));
					String creditType = sourceCredits
							.getString(sourceCredits
									.getColumnIndexOrThrow(com.bubblesworth.soundboard.pack.CreditColumns.CREDIT_TYPE));
					creditValues.put(CREDIT_TYPE, creditType);
					String creditLink = sourceCredits
							.getString(sourceCredits
									.getColumnIndexOrThrow(com.bubblesworth.soundboard.pack.CreditColumns.CREDIT_LINK));
					creditValues.put(CREDIT_LINK, creditLink);
					// Credit display ordering
					int order;
					if (creditType.equalsIgnoreCase("header")) {
						order = 0;
					} else if (creditType.equalsIgnoreCase("code")) {
						order = 10;
					} else if (creditType.equalsIgnoreCase("sounds")) {
						order = 20;
					} else if (creditType.equalsIgnoreCase("series")) {
						order = 30;
					} else if (creditType.equalsIgnoreCase("trademark")) {
						order = 40;
					} else if (creditType.equalsIgnoreCase("music")) {
						order = 50;
					} else if (creditType.equalsIgnoreCase("art")) {
						order = 60;
					} else {
						Log.e(TAG, "Unexpected credit type " + creditType);
						order = 100;
					}
					if (creditLink.length() != 0) {
						order += 5;
					}
					creditValues.put(CREDIT_ORDER, order);
					db.insertOrThrow(CREDIT_TABLE, null, creditValues);
				} while (sourceCredits.moveToNext());
			}
		} catch (Exception e) {
			Log.e(TAG, "error in importPack", e);
			deletePack(db, sourceId);
		} finally {
			sourceCategories.close();
			sourceSounds.close();
			sourceCredits.close();
		}
	}

	private void deletePack(SQLiteDatabase db, long packId) {
		// Log.d(TAG, "Deleting by packId " + packId);
		SQLiteQueryBuilder categoryQuery = new SQLiteQueryBuilder();
		categoryQuery.setTables(CATEGORY_TABLE);
		categoryQuery.appendWhere(SOURCE_ID + "=?");
		Cursor categories = categoryQuery.query(db, new String[] { _ID }, null,
				new String[] { "" + packId }, null, null, null);
		if (categories.moveToFirst()) {
			StringBuilder categoriesMatch = new StringBuilder();
			do {
				categoriesMatch.append(categories.getLong(categories
						.getColumnIndexOrThrow(_ID)));
				if (!categories.isLast())
					categoriesMatch.append(",");
			} while (categories.moveToNext());
			String categoriesList = categoriesMatch.toString();
			// Can't use the whereArgs for this, it gets string-escaped.
			/* int soundRows = */db.delete(SOUND_TABLE, CATEGORY_ID + " IN ("
					+ categoriesList + ")", null);
			/* int catRows = */db.delete(CATEGORY_TABLE, _ID + " IN ("
					+ categoriesList + ")", null);
			// Log.d(TAG, "Deleted " + soundRows + " sounds and " + catRows +
			// " categories" );
		}
		db.delete(CREDIT_TABLE, SOURCE_ID + " = ?",
				new String[] { "" + packId });
		db.delete(SOURCE_TABLE, _ID + " = ?", new String[] { "" + packId });
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

		String groupBy = null;
		int match = URI_MATCHER.match(uri);
		switch (match) {
		case TRACKS_ID:
			qb.appendWhere(_ID + "=" + ContentUris.parseId(uri));
			// Fall-through
		case TRACKS:
			qb.setTables(SOUND_TABLE);
			if (TextUtils.isEmpty(sortOrder)) {
				sortOrder = CATEGORY_ID + " ASC, " + SoundColumns.DESCRIPTION
						+ " ASC";
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
				// Hide the SOURCE_ID column
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
				sortOrder = CREDIT_ORDER + " ASC, " + CREDIT_NAME + " ASC";
			}
			if (projection == null) {
				// Hide the SOURCE_ID column
				projection = new String[] { _ID, CREDIT_TYPE, CREDIT_NAME,
						CREDIT_LINK, CREDIT_ORDER };
			}
			groupBy = CREDIT_ORDER + ", " + CREDIT_NAME;
			HashMap<String, String> projectionMap = new HashMap<String, String>();
			projectionMap.put(_ID, "MIN(" + _ID + ") AS " + _ID);
			projectionMap.put(SOURCE_ID, "MIN(" + SOURCE_ID + ") AS "
					+ SOURCE_ID);
			projectionMap.put(CREDIT_TYPE, "MIN(" + CREDIT_TYPE + ") AS "
					+ CREDIT_TYPE);
			projectionMap.put(CREDIT_NAME, CREDIT_NAME);
			projectionMap.put(CREDIT_LINK, "MIN(" + CREDIT_LINK + ") AS "
					+ CREDIT_LINK);
			projectionMap.put(CREDIT_ORDER, CREDIT_ORDER);
			qb.setProjectionMap(projectionMap);
			break;
		case ASSETS_ID:
		case ICONS_ID:
		default:
			return null;
		}
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy,
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
		// TODO: Favourites support
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#openAssetFile(android.net.Uri,
	 * java.lang.String)
	 */
	// Note that this is now a legacy API. AssetFileDescriptor URIs now
	// point directly to their content pack.
	// However, existing AppWidgets will be pointing at these icons
	// and asset URIs until they are recreated .
	@Override
	public AssetFileDescriptor openAssetFile(Uri uri, String mode)
			throws FileNotFoundException {
		loadData();
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		long id = ContentUris.parseId(uri);

		// See importPack...
		if (id < 1000) {
			qb.setTables(CATEGORY_TABLE);
		} else {
			qb.setTables(SOUND_TABLE);
		}

		qb.appendWhere(_ID + "=" + id);

		SQLiteDatabase db = dbHelper.getReadableDatabase();

		String column;
		int match = URI_MATCHER.match(uri);
		switch (match) {
		case ASSETS_ID:
			column = ASSET;
			break;

		case ICONS_ID:
			if (id < 1000) {
				column = CategoryColumns.ICON;
			} else {
				column = SoundColumns.ICON;
			}
			break;

		default:
			throw new FileNotFoundException();
		}
		Cursor c = qb.query(db, new String[] { column }, null, null, null,
				null, null);

		if (!c.moveToFirst())
			throw new FileNotFoundException();

		return getContext().getContentResolver().openAssetFileDescriptor(
				Uri.parse(c.getString(c.getColumnIndexOrThrow(column))), mode);
	}
}
