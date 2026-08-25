package dev.fimkov.echoesbeneath.memory;

public final class EchoFormat {
	public static final int CURRENT_VERSION = 1;
	public static final int MINIMUM_SUPPORTED_VERSION = 1;
	public static final int MAX_MEMORY_SLOTS = 3;
	public static final int DEFAULT_MEMORY_SLOTS = 1;

	private EchoFormat() {
	}

	public static boolean isSupported(int version) {
		return version >= MINIMUM_SUPPORTED_VERSION && version <= CURRENT_VERSION;
	}
}
