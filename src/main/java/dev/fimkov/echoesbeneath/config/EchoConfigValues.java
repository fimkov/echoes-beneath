package dev.fimkov.echoesbeneath.config;

public record EchoConfigValues(
		int recordingRadius,
		int recordingVerticalRadius,
		int maxRecordingSeconds,
		int entitySampleRate,
		int maxRecordedEntities,
		int maxSoundEvents,
		int maxParticleEvents,
		int maxConcurrentSoundWaves,
		boolean allowOverlappingPlayback,
		boolean suppressHostileMobs,
		boolean disableBlockInteractionDuringPlayback,
		boolean allowMemoryMending,
		int mendingExperienceCost) {

	private static final int TICKS_PER_SECOND = 20;

	public int maxRecordingTicks() {
		return maxRecordingSeconds * TICKS_PER_SECOND;
	}

	public int entitySampleIntervalTicks() {
		return Math.max(1, TICKS_PER_SECOND / entitySampleRate);
	}
}
