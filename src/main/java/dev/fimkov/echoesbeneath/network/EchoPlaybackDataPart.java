package dev.fimkov.echoesbeneath.network;

public enum EchoPlaybackDataPart {
	SNAPSHOT,
	BLOCK_TIMELINE,
	ENTITY_TIMELINE,
	SOUND_TIMELINE,
	PARTICLE_TIMELINE;

	private static final EchoPlaybackDataPart[] VALUES = values();

	public static EchoPlaybackDataPart fromIndex(int index) {
		if (index < 0 || index >= VALUES.length) {
			throw new IllegalArgumentException("Unknown playback data part " + index);
		}
		return VALUES[index];
	}
}
