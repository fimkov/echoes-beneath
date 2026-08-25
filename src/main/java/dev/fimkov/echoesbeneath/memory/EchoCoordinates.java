package dev.fimkov.echoesbeneath.memory;

import net.minecraft.core.BlockPos;

public final class EchoCoordinates {
	public static final int COORDINATE_BITS = 9;
	public static final int COORDINATE_MASK = (1 << COORDINATE_BITS) - 1;
	public static final int COORDINATE_BIAS = 1 << (COORDINATE_BITS - 1);
	public static final int POSITION_QUANTIZATION = 256;
	public static final int ROTATION_QUANTIZATION = 256;
	private static final float DEGREES_IN_TURN = 360.0F;

	private EchoCoordinates() {
	}

	public static int packRelative(int x, int y, int z) {
		return ((x + COORDINATE_BIAS) & COORDINATE_MASK)
				| (((y + COORDINATE_BIAS) & COORDINATE_MASK) << COORDINATE_BITS)
				| (((z + COORDINATE_BIAS) & COORDINATE_MASK) << (COORDINATE_BITS * 2));
	}

	public static int unpackX(int packed) {
		return (packed & COORDINATE_MASK) - COORDINATE_BIAS;
	}

	public static int unpackY(int packed) {
		return ((packed >> COORDINATE_BITS) & COORDINATE_MASK) - COORDINATE_BIAS;
	}

	public static int unpackZ(int packed) {
		return ((packed >> (COORDINATE_BITS * 2)) & COORDINATE_MASK) - COORDINATE_BIAS;
	}

	public static boolean fitsInPackedRange(int value) {
		return value >= -COORDINATE_BIAS && value < COORDINATE_BIAS;
	}

	public static int quantizePosition(double relative) {
		return (int) Math.round(relative * POSITION_QUANTIZATION);
	}

	public static double dequantizePosition(int quantized) {
		return (double) quantized / POSITION_QUANTIZATION;
	}

	public static int quantizeRotation(float degrees) {
		return Math.round(degrees / DEGREES_IN_TURN * ROTATION_QUANTIZATION);
	}

	public static float dequantizeRotation(int quantized) {
		return (float) quantized / ROTATION_QUANTIZATION * DEGREES_IN_TURN;
	}

	public static BlockPos relativeTo(BlockPos center, int relativeX, int relativeY, int relativeZ) {
		return center.offset(relativeX, relativeY, relativeZ);
	}
}
