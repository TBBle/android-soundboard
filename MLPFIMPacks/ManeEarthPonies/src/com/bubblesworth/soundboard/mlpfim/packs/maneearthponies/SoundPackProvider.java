/**
 * 
 */
package com.bubblesworth.soundboard.mlpfim.packs.maneearthponies;

/**
 * @author tbble
 * 
 */
public class SoundPackProvider extends
		com.bubblesworth.soundboard.pack.common.SoundPackProvider {
	private static final String AUTHORITY = "com.bubblesworth.soundboard.mlpfim.packs.maneearthponies";

	@Override
	protected String getAuthority() {
		return AUTHORITY;
	}

	@Override
	protected int getSoundsResource() {
		return R.xml.sounds;
	}

	@Override
	protected int getCreditsResource() {
		return R.xml.credits;
	}
}
