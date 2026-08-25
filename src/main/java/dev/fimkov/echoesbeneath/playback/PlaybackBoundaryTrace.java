package dev.fimkov.echoesbeneath.playback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

public final class PlaybackBoundaryTrace {
	public static final int BEAT_INTERVAL_TICKS = 2;

	private static final int ARC_POINTS = 26;
	private static final double ARC_HALF_WIDTH_RADIANS = Math.PI / 3.0D;
	private static final double DRIFT_RADIANS_PER_BEAT = 0.02D;
	private static final double VISIBLE_MARGIN = 48.0D;
	private static final double SURFACE_OFFSET = 0.15D;
	private static final int ACCENT_EVERY = 6;
	private static final int PARTICLE_COUNT = 1;
	private static final double NO_SPREAD = 0.0D;
	private static final double NO_SPEED = 0.0D;
	private static final float CHARGE_ROLL_SCALE = 0.5F;

	private PlaybackBoundaryTrace() {
	}

	public static void sweep(ServerLevel level, EchoRegion region, int tick) {
		BlockPos center = region.center();
		double radius = region.radius();
		for (ServerPlayer viewer : level.players()) {
			double toViewerX = viewer.getX() - (center.getX() + 0.5D);
			double toViewerZ = viewer.getZ() - (center.getZ() + 0.5D);
			double distance = Math.sqrt(toViewerX * toViewerX + toViewerZ * toViewerZ);
			if (Math.abs(distance - radius) > VISIBLE_MARGIN) {
				continue;
			}
			emitArc(level, viewer, center, radius, Math.atan2(toViewerZ, toViewerX), tick);
		}
	}

	private static void emitArc(ServerLevel level, ServerPlayer viewer, BlockPos center,
			double radius, double facingAngle, int tick) {
		double drift = tick * DRIFT_RADIANS_PER_BEAT;
		for (int point = 0; point < ARC_POINTS; point++) {
			double offset = (point / (double) (ARC_POINTS - 1) - 0.5D) * 2.0D * ARC_HALF_WIDTH_RADIANS;
			double angle = facingAngle + offset + Math.sin(drift + point) * 0.02D;
			double x = center.getX() + 0.5D + Math.cos(angle) * radius;
			double z = center.getZ() + 0.5D + Math.sin(angle) * radius;
			int columnX = (int) Math.floor(x);
			int columnZ = (int) Math.floor(z);
			if (!level.hasChunkAt(new BlockPos(columnX, center.getY(), columnZ))) {
				continue;
			}
			double surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, columnX, columnZ);
			double y = surface + SURFACE_OFFSET + Math.abs(Math.sin(drift * 3.0D + point)) * 1.6D;
			ParticleOptions particle = point % ACCENT_EVERY == 0
					? ParticleTypes.SCULK_SOUL
					: new SculkChargeParticleOptions((float) Math.cos(angle) * CHARGE_ROLL_SCALE);
			level.sendParticles(viewer, particle, true, x, y, z,
					PARTICLE_COUNT, NO_SPREAD, NO_SPREAD, NO_SPREAD, NO_SPEED);
		}
	}
}
