package dev.fimkov.echoesbeneath.recording;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

public final class RecordingPulseTrace {
	public static final int BEAT_INTERVAL_TICKS = 1;

	private static final double PULSE_SPEED_BLOCKS_PER_TICK = 0.85D;
	private static final int CONCURRENT_RINGS = 3;
	private static final double POINTS_PER_BLOCK_OF_RADIUS = 2.2D;
	private static final int MIN_POINTS = 14;
	private static final int MAX_POINTS = 96;
	private static final double SURFACE_OFFSET = 1.04D;
	private static final int ACCENT_EVERY = 11;
	private static final float CHARGE_ROLL_SCALE = 0.35F;
	private static final int PARTICLE_COUNT = 1;
	private static final double NO_SPREAD = 0.0D;
	private static final double NO_SPEED = 0.0D;
	private static final double MIN_VISIBLE_RADIUS = 1.5D;

	private RecordingPulseTrace() {
	}

	public static void sweep(ServerLevel level, ServerPlayer viewer, BlockPos center,
			int radius, int verticalRadius, int tick) {
		if (viewer == null) {
			return;
		}

		double cycle = radius / PULSE_SPEED_BLOCKS_PER_TICK;

		for (int ring = 0; ring < CONCURRENT_RINGS; ring++) {
			double phase = (tick + ring * cycle / CONCURRENT_RINGS) % cycle;
			double ringRadius = phase * PULSE_SPEED_BLOCKS_PER_TICK;

			if (ringRadius < MIN_VISIBLE_RADIUS || ringRadius > radius) {
				continue;
			}
			emitRing(level, viewer, center, ringRadius, verticalRadius, tick + ring);
		}
	}

	private static void emitRing(ServerLevel level, ServerPlayer viewer, BlockPos center,
			double ringRadius, int verticalRadius, int seed) {
		int points = (int) Math.clamp(Math.round(ringRadius * POINTS_PER_BLOCK_OF_RADIUS), MIN_POINTS, MAX_POINTS);
		double angleStep = Math.PI * 2.0D / points;
		double angleOffset = seed * 0.017D;

		for (int point = 0; point < points; point++) {
			double angle = angleOffset + point * angleStep;
			double x = center.getX() + 0.5D + Math.cos(angle) * ringRadius;
			double z = center.getZ() + 0.5D + Math.sin(angle) * ringRadius;
			int columnX = (int) Math.floor(x);
			int columnZ = (int) Math.floor(z);

			if (!level.hasChunkAt(new BlockPos(columnX, center.getY(), columnZ))) {
				continue;
			}
			int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, columnX, columnZ);

			if (Math.abs(surface - center.getY()) > verticalRadius) {
				continue;
			}
			ParticleOptions particle = point % ACCENT_EVERY == 0
					? ParticleTypes.SCULK_SOUL
					: new SculkChargeParticleOptions((float) Math.sin(angle) * CHARGE_ROLL_SCALE);
			level.sendParticles(viewer, particle, true, false, x, surface + SURFACE_OFFSET - 1.0D, z,
					PARTICLE_COUNT, NO_SPREAD, NO_SPREAD, NO_SPREAD, NO_SPEED);
		}
	}
}
