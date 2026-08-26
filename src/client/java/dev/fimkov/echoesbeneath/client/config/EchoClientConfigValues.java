package dev.fimkov.echoesbeneath.client.config;

public record EchoClientConfigValues(
		int waveTexelBudget,
		int hologramBlockBudget,
		int hologramDistance,
		boolean showBlockHolograms,
		boolean hideSkyDuringPlayback,
		int transitionTicks,
		int regionAmbienceVolume,
		int waveDetail,
		boolean showChartAura,
		int chartDistance,
		int chartBlockBudget,
		boolean tableAnimations,
		int darknessStrength,
		int waveVolume) {

	private static final float PERCENT = 100.0F;
	private static final float TICKS_TO_RATE = 1.0F;

	public float regionAmbienceGain() {
		return regionAmbienceVolume / PERCENT;
	}

	public float darknessFactor() {
		return darknessStrength / PERCENT;
	}

	public float waveGain() {
		return waveVolume / PERCENT;
	}

	public float transitionPerTick() {
		return TICKS_TO_RATE / Math.max(1, transitionTicks);
	}
}
