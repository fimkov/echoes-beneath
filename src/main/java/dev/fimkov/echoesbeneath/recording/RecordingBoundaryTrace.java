package dev.fimkov.echoesbeneath.recording;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

public final class RecordingBoundaryTrace {
	public static final int BEAT_INTERVAL_TICKS = 2;
	private static final int POINTS_PER_BEAT = 7;
	private static final double SWEEP_RADIANS_PER_BEAT = 0.09D;
	private static final double SPOKE_SPACING_RADIANS = Math.PI * 2.0D / POINTS_PER_BEAT;
	private static final double SURFACE_OFFSET = 0.55D;
	private static final int PARTICLE_COUNT = 1;
	private static final double PARTICLE_SPREAD = 0.0D;
	private static final double PARTICLE_SPEED = 0.0D;
	private static final int MARKER_EVERY_POINTS = 7;

	private RecordingBoundaryTrace() {
	}

	public static void sweep(ServerLevel level, ServerPlayer viewer, BlockPos center,
			int radius, int verticalRadius, int beat) {
		if (viewer == null) {
			return;
		}
		double sweepOffset = beat * SWEEP_RADIANS_PER_BEAT;
		for (int point = 0; point < POINTS_PER_BEAT; point++) {
			double angle = sweepOffset + point * SPOKE_SPACING_RADIANS;
			double x = center.getX() + 0.5D + Math.cos(angle) * radius;
			double z = center.getZ() + 0.5D + Math.sin(angle) * radius;
			double y = surfaceHeight(level, center, verticalRadius, x, z);
			ParticleOptions particle = (beat + point) % MARKER_EVERY_POINTS == 0
					? ParticleTypes.SCULK_SOUL
					: ParticleTypes.ELECTRIC_SPARK;
			level.sendParticles(viewer, particle, true, x, y, z,
					PARTICLE_COUNT, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPEED);
		}
	}

	private static double surfaceHeight(ServerLevel level, BlockPos center, int verticalRadius,
			double x, double z) {
		int columnX = (int) Math.floor(x);
		int columnZ = (int) Math.floor(z);
		if (!level.hasChunkAt(new BlockPos(columnX, center.getY(), columnZ))) {
			return center.getY() + SURFACE_OFFSET;
		}
		int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, columnX, columnZ);
		int clamped = Math.clamp(surface, center.getY() - verticalRadius, center.getY() + verticalRadius);
		return clamped + SURFACE_OFFSET;
	}
}
