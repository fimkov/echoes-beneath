package dev.fimkov.echoesbeneath.network;

public final class EchoPayloadLimits {
	public static final int MAX_CHUNK_BYTES = 30000;
	public static final int MAX_CHUNKS_PER_PART = 4096;
	public static final int MAX_RADIUS = 128;
	public static final int MAX_DURATION_TICKS = 20 * 60 * 10;

	private EchoPayloadLimits() {
	}
}
