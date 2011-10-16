package com.bubblesworth.soundboard.pack;

import android.provider.BaseColumns;

public interface CategoryColumns extends BaseColumns {
	// We reflect _ID and _COUNT from BaseColumns
	public static final String DESCRIPTION = "description"; // String
	public static final String ICON = "icon"; // String: URI for
												// ContentResolver.openAssetFileDescriptor
}