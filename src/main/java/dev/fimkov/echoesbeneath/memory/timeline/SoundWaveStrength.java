package dev.fimkov.echoesbeneath.memory.timeline;

import net.minecraft.sounds.SoundSource;

public final class SoundWaveStrength {
	public static final float WAVE_SPEED_BLOCKS_PER_TICK = 0.9F;
	private static final float MINIMUM_RADIUS = 3.0F;
	private static final float MAXIMUM_RADIUS = 50.0F;
	private static final float VOLUME_SCALE = 7.0F;
	private static final float VOLUME_BASE = 2.0F;
	private static final float HOSTILE_BONUS = 1.35F;
	private static final float BLOCK_BONUS = 1.1F;
	private static final float PLAYER_BONUS = 1.0F;
	private static final float AMBIENT_PENALTY = 0.7F;

	private SoundWaveStrength() {
	}

	public static float radiusFor(SoundSource source, float volume) {
		float base = Math.max(0.1F, volume) * VOLUME_SCALE + VOLUME_BASE;
		float scaled = base * categoryFactor(source);
		return Math.clamp(scaled, MINIMUM_RADIUS, MAXIMUM_RADIUS);
	}

	private static float categoryFactor(SoundSource source) {
		return switch (source) {
			case HOSTILE -> HOSTILE_BONUS;
			case BLOCKS -> BLOCK_BONUS;
			case PLAYERS, NEUTRAL -> PLAYER_BONUS;
			default -> AMBIENT_PENALTY;
		};
	}

	public static int lifetimeTicks(float radius) {
		return Math.max(1, Math.round(radius / WAVE_SPEED_BLOCKS_PER_TICK));
	}
}
