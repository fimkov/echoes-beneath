package dev.fimkov.echoesbeneath.client.render;

import dev.fimkov.echoesbeneath.client.config.EchoClientConfig;

public final class EchoTexelGrid {

	public static final int TICKS_PER_ANIMATION_STEP = 3;


	private static final int HASH_X = 73856093;
	private static final int HASH_Y = 19349663;
	private static final int HASH_Z = 83492791;
	private static final int ANIMATION_SALT = 40503;
	private static final int HASH_RANGE = 0xFFFF;
	private static final float GLOW_SHARE = 0.035F;
	private static final float EMBER_SHARE = 0.075F;

	private EchoTexelGrid() {
	}

	public static int texelsPerBlock() {
		return EchoClientConfig.values().waveDetail();
	}

	public static float texelSize() {
		return 1.0F / texelsPerBlock();
	}

	public static int hash(double worldX, double worldY, double worldZ, int animationStep) {
		int cellX = (int) Math.floor(worldX * texelsPerBlock());
		int cellY = (int) Math.floor(worldY * texelsPerBlock());
		int cellZ = (int) Math.floor(worldZ * texelsPerBlock());
		int mixed = cellX * HASH_X ^ cellY * HASH_Y ^ cellZ * HASH_Z ^ animationStep * ANIMATION_SALT;
		return (mixed ^ (mixed >>> 13)) & HASH_RANGE;
	}

	public static boolean isGlow(int hash) {
		return share(hash) < GLOW_SHARE;
	}

	public static boolean isEmber(int hash) {
		float value = share(hash);
		return value >= GLOW_SHARE && value < EMBER_SHARE;
	}

	private static float share(int hash) {
		return (float) hash / HASH_RANGE;
	}
}
