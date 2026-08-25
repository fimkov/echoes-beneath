package dev.fimkov.echoesbeneath.client.wave;

import net.minecraft.sounds.SoundSource;

public final class EchoWavePalette {
	public static final int PALETTE_SCULK = 0;
	public static final int PALETTE_AMETHYST = 1;

	public static final float FRONT_DEPTH = 0.06F;

	private static final int[] OUTLINE_COLOURS = {0xD9F6F6, 0xF2DFFF};
	private static final int[] GLOW_COLOURS = {0x29DFEB, 0xCFA0F3};
	private static final int[] EMBER_COLOURS = {0x009295, 0x8D6ACC};

	private static final int[][] BODY_STOPS = {
			{0x052A32, 0x0B212A, 0x111B21, 0x0D1217},
			{0x241A3C, 0x1D1530, 0x161024, 0x0F0B18},
	};

	private static final int OUTLINE_ALPHA = 225;
	private static final int BODY_ALPHA = 165;
	private static final int GLOW_ALPHA = 240;
	private static final int ALPHA_STEPS = 5;

	private EchoWavePalette() {
	}

	public static int paletteFor(SoundSource source) {
		return source == SoundSource.HOSTILE ? PALETTE_AMETHYST : PALETTE_SCULK;
	}

	public static boolean isFront(float depth) {
		return depth <= FRONT_DEPTH;
	}

	public static int outlineColour(int paletteIndex, float fade) {
		int alpha = quantise(OUTLINE_ALPHA * Math.clamp(fade, 0.0F, 1.0F));
		return (alpha << 24) | OUTLINE_COLOURS[clampPalette(paletteIndex)];
	}

	public static int glowColour(int paletteIndex, float fade, boolean ember) {
		int alpha = quantise(GLOW_ALPHA * Math.clamp(fade, 0.0F, 1.0F));
		int[] source = ember ? EMBER_COLOURS : GLOW_COLOURS;
		return (alpha << 24) | source[clampPalette(paletteIndex)];
	}

	public static int colorAt(int paletteIndex, float depth, float fade) {
		int[] stops = BODY_STOPS[clampPalette(paletteIndex)];
		float clamped = Math.clamp(depth, 0.0F, 1.0F);
		int stop = Math.min(stops.length - 1, (int) (clamped * stops.length));
		float tail = 1.0F - clamped;
		int alpha = quantise(BODY_ALPHA * tail * Math.clamp(fade, 0.0F, 1.0F));
		return (alpha << 24) | stops[stop];
	}

	private static int clampPalette(int paletteIndex) {
		return Math.clamp(paletteIndex, 0, OUTLINE_COLOURS.length - 1);
	}

	private static int quantise(float raw) {
		int step = Math.round(Math.clamp(raw / 255.0F, 0.0F, 1.0F) * ALPHA_STEPS);
		return Math.round(255.0F * step / ALPHA_STEPS);
	}
}
