package dev.fimkov.echoesbeneath.client.wave;

import net.minecraft.sounds.SoundSource;

public final class EchoWavePalette {
	public static final int PALETTE_CYAN = 0;
	public static final int PALETTE_VIOLET = 1;
	public static final int PALETTE_GOLD = 2;
	public static final int PALETTE_GREEN = 3;
	public static final int PALETTE_PALE = 4;

	private static final int[][] PALETTES = {
			{0xE8FFFF, 0x6FE3E0, 0x009295, 0x1B3E8C, 0x5B3AA6},
			{0xFFF0FF, 0xE7A6FF, 0xA254E8, 0x5B2CA6, 0x2A1560},
			{0xFFFDF0, 0xFFD98A, 0xE8A33D, 0x9C5A1E, 0x45210A},
			{0xF0FFF4, 0x9CF5B4, 0x3FBF7A, 0x1A6B4A, 0x0C2E28},
			{0xFFFFFF, 0xCFE0FF, 0x7A9BD6, 0x33477F, 0x141C33},
	};

	private static final float MAX_ALPHA = 235.0F;
	private static final float FRONT_RAMP = 0.14F;

	private EchoWavePalette() {
	}

	public static int paletteFor(SoundSource source) {
		return switch (source) {
			case BLOCKS -> PALETTE_CYAN;
			case HOSTILE -> PALETTE_VIOLET;
			case PLAYERS -> PALETTE_GOLD;
			case NEUTRAL -> PALETTE_GREEN;
			default -> PALETTE_PALE;
		};
	}

	public static int colorAt(int paletteIndex, float depth, float fade) {
		int[] stops = PALETTES[Math.clamp(paletteIndex, 0, PALETTES.length - 1)];
		float clamped = Math.clamp(depth, 0.0F, 1.0F);
		float scaled = clamped * (stops.length - 1);
		int lower = (int) scaled;
		int upper = Math.min(lower + 1, stops.length - 1);
		float blend = scaled - lower;
		int red = mix(channel(stops[lower], 16), channel(stops[upper], 16), blend);
		int green = mix(channel(stops[lower], 8), channel(stops[upper], 8), blend);
		int blue = mix(channel(stops[lower], 0), channel(stops[upper], 0), blend);
		float frontRamp = Math.min(1.0F, clamped / FRONT_RAMP);
		float tail = (1.0F - clamped) * (1.0F - clamped);
		int alpha = Math.round(MAX_ALPHA * frontRamp * tail * Math.clamp(fade, 0.0F, 1.0F));
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}

	private static int mix(int from, int to, float blend) {
		return Math.clamp(Math.round(from + (to - from) * blend), 0, 255);
	}

	private static int channel(int color, int shift) {
		return (color >> shift) & 0xFF;
	}
}
