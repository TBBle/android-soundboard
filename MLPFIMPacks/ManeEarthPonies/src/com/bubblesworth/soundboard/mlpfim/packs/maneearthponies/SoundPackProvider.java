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

	protected String getAuthority() {
		return AUTHORITY;
	}

	protected int getSoundsResource() {
		return R.xml.sounds;
	}

	protected int getCreditsResource() {
		return R.xml.credits;
	}
}
