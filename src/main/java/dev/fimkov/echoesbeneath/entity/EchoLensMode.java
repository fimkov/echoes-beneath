package dev.fimkov.echoesbeneath.entity;

public enum EchoLensMode {
	RECORDING,
	PLAYBACK,
	RETURNING,
	ABANDONED;

	private static final EchoLensMode[] VALUES = values();

	public static EchoLensMode fromIndex(int index) {
		if (index < 0 || index >= VALUES.length) {
			return ABANDONED;
		}
		return VALUES[index];
	}
}
