/**
 * 
 */
package com.bubblesworth.soundboard.mlpfim.packs.manepegasi;

/**
 * @author tbble
 * 
 */
public class SoundPackProvider extends
		com.bubblesworth.soundboard.pack.common.SoundPackProvider {
	private static final String AUTHORITY = "com.bubblesworth.soundboard.mlpfim.packs.manepegasi";

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