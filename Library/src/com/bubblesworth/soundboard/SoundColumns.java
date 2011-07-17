package com.bubblesworth.soundboard;

import android.provider.BaseColumns;


public interface SoundColumns extends BaseColumns {
	// We reflect _ID and _COUNT from BaseColumns
	public static final String DESCRIPTION = "description";
	public static final String ACTION = "action";
	public static final String ASSET = "asset";

}
