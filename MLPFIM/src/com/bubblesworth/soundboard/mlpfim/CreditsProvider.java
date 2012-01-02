package com.bubblesworth.soundboard.mlpfim;

import com.bubblesworth.soundboard.pack.common.SoundPackProvider;

public class CreditsProvider extends SoundPackProvider {
	private static final String AUTHORITY = "com.bubblesworth.soundboard.mlpfim.creditsprovider";

	@Override
	protected String getAuthority() {
		return AUTHORITY;
	}

	@Override
	protected int getSoundsResource() {
		return 0;
	}

	@Override
	protected int getSoundsCount() {
		return 0;
	}

	@Override
	protected int getCreditsResource() {
		return R.xml.credits;
	}

	@Override
	protected int getCreditsCount() {
		return 7;
	}
}
