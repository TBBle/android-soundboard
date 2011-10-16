package com.bubblesworth.soundboard.pack;

import android.provider.BaseColumns;

public interface CreditColumns extends BaseColumns {
	// We reflect _ID and _COUNT from BaseColumns
	public static final String CREDIT_TYPE = "credit_type";
	public static final String CREDIT_NAME = "credit_name";
	public static final String CREDIT_LINK = "credit_link";
}
