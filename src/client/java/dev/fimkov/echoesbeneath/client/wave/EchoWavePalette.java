package dev.fimkov.echoesbeneath.client.wave;

import net.minecraft.sounds.SoundSource;

public final class EchoWavePalette {
	public static final int PALETTE_SCULK = 0;
	public static final int PALETTE_AMETHYST = 1;
	public static final int PALETTE_SENSOR = 2;

	private static final int[][] PALETTES = {
			{0x29DFEB, 0x009295, 0x0A5060, 0x034150, 0x052A32},
			{0xCFA0F3, 0xB38EF3, 0x8D6ACC, 0x6F4FAB, 0x54398A},
			{0x009295, 0x05625D, 0x074857, 0x034150, 0x052A32},
	};

	private static final int ALPHA_STEPS = 6;
	private static final float MAX_ALPHA = 170.0F;
	private static final float FRONT_RAMP = 0.16F;

	private EchoWavePalette() {
	}

	public static int paletteFor(SoundSource source) {
		return switch (source) {
			case HOSTILE -> PALETTE_AMETHYST;
			case PLAYERS, NEUTRAL -> PALETTE_SENSOR;
			default -> PALETTE_SCULK;
		};
	}

	public static int colorAt(int paletteIndex, float depth, float fade) {
		int[] stops = PALETTES[Math.clamp(paletteIndex, 0, PALETTES.length - 1)];
		float clamped = Math.clamp(depth, 0.0F, 1.0F);
		int stop = Math.min(stops.length - 1, (int) (clamped * stops.length));
		int color = stops[stop];
		float frontRamp = Math.min(1.0F, clamped / FRONT_RAMP);
		float tail = (1.0F - clamped) * (1.0F - clamped);
		float raw = frontRamp * tail * Math.clamp(fade, 0.0F, 1.0F);
		int alpha = quantiseAlpha(raw);
		return (alpha << 24) | color;
	}

	private static int quantiseAlpha(float raw) {
		int step = Math.round(Math.clamp(raw, 0.0F, 1.0F) * ALPHA_STEPS);
		return Math.round(MAX_ALPHA * step / (float) ALPHA_STEPS);
	}
}
