package com.bubblesworth.soundboard;

import android.provider.BaseColumns;

public interface SoundColumns extends BaseColumns {
	// We reflect _ID and _COUNT from BaseColumns
	public static final String CATEGORY_ID = "category_id"; // Integer
	public static final String DESCRIPTION = "description"; // String
	public static final String ACTION = "action"; // String
	public static final String ASSET = "asset"; // String: URI for
												// ContentResolver.openAssetFileDescriptor
	public static final String ICON = "icon"; // String: URI for
												// ContentResolver.openAssetFileDescriptor
}
