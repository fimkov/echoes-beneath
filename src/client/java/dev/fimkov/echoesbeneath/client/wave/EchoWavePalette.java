package dev.fimkov.echoesbeneath.client.wave;

public final class EchoWavePalette {
	private static final int[] STOP_COLORS = {
			0xE8FFFF,
			0x6FE3E0,
			0x009295,
			0x1B3E8C,
			0x5B3AA6
	};
	private static final float MAX_ALPHA = 195.0F;
	private static final float FRONT_RAMP = 0.14F;

	private EchoWavePalette() {
	}

	public static int colorAt(float depth, float fade) {
		float clamped = Math.clamp(depth, 0.0F, 1.0F);
		float scaled = clamped * (STOP_COLORS.length - 1);
		int lower = (int) scaled;
		int upper = Math.min(lower + 1, STOP_COLORS.length - 1);
		float blend = scaled - lower;
		int red = mix(red(STOP_COLORS[lower]), red(STOP_COLORS[upper]), blend);
		int green = mix(green(STOP_COLORS[lower]), green(STOP_COLORS[upper]), blend);
		int blue = mix(blue(STOP_COLORS[lower]), blue(STOP_COLORS[upper]), blend);
		float frontRamp = Math.min(1.0F, clamped / FRONT_RAMP);
		float tail = (1.0F - clamped) * (1.0F - clamped) * (1.0F - clamped);
		int alpha = Math.round(MAX_ALPHA * frontRamp * tail * Math.clamp(fade, 0.0F, 1.0F));
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}

	private static int mix(int from, int to, float blend) {
		return Math.clamp(Math.round(from + (to - from) * blend), 0, 255);
	}

	private static int red(int color) {
		return (color >> 16) & 0xFF;
	}

	private static int green(int color) {
		return (color >> 8) & 0xFF;
	}

	private static int blue(int color) {
		return color & 0xFF;
	}
}
