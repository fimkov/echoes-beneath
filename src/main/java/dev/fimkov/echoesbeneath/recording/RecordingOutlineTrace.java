package dev.fimkov.echoesbeneath.recording;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class RecordingOutlineTrace {
	public static final int BEAT_INTERVAL_TICKS = 2;

	private static final int WORKING_RADIUS = 14;
	private static final int SAMPLE_ATTEMPTS = 120;
	private static final int MIN_PARTICLES_PER_BEAT = 6;
	private static final int MAX_PARTICLES_PER_BEAT = 30;
	private static final double PULSE_PERIOD_TICKS = 36.0D;
	private static final double FACE_OFFSET = 0.02D;
	private static final int ACCENT_EVERY = 9;
	private static final int PARTICLE_COUNT = 1;
	private static final double NO_SPREAD = 0.0D;
	private static final double NO_SPEED = 0.0D;
	private static final Direction[] DIRECTIONS = Direction.values();

	private RecordingOutlineTrace() {
	}

	public static void sweep(ServerLevel level, ServerPlayer viewer, BlockPos center,
			int radius, int verticalRadius, int tick) {
		if (viewer == null) {
			return;
		}
		RandomSource random = level.random;
		int budget = pulsedBudget(tick);
		BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
		BlockPos viewerPos = viewer.blockPosition();
		int spawned = 0;

		for (int attempt = 0; attempt < SAMPLE_ATTEMPTS && spawned < budget; attempt++) {
			probe.set(
					viewerPos.getX() + random.nextInt(WORKING_RADIUS * 2 + 1) - WORKING_RADIUS,
					viewerPos.getY() + random.nextInt(WORKING_RADIUS * 2 + 1) - WORKING_RADIUS,
					viewerPos.getZ() + random.nextInt(WORKING_RADIUS * 2 + 1) - WORKING_RADIUS);
			if (!withinRecordedRegion(probe, center, radius, verticalRadius) || !level.hasChunkAt(probe)) {
				continue;
			}
			BlockState state = level.getBlockState(probe);
			if (state.isAir() || !state.isSolidRender(level, probe)) {
				continue;
			}
			Direction face = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
			if (!level.getBlockState(probe.relative(face)).isAir()) {
				continue;
			}
			Vec3 point = edgePoint(probe, face, random);
			ParticleOptions particle = spawned % ACCENT_EVERY == 0
					? ParticleTypes.SCULK_SOUL
					: ParticleTypes.ELECTRIC_SPARK;
			level.sendParticles(viewer, particle, true, point.x, point.y, point.z,
					PARTICLE_COUNT, NO_SPREAD, NO_SPREAD, NO_SPREAD, NO_SPEED);
			spawned++;
		}
	}

	private static int pulsedBudget(int tick) {
		double pulse = 0.5D + 0.5D * Math.sin(tick / PULSE_PERIOD_TICKS * Math.PI * 2.0D);
		return MIN_PARTICLES_PER_BEAT
				+ (int) Math.round((MAX_PARTICLES_PER_BEAT - MIN_PARTICLES_PER_BEAT) * pulse);
	}

	private static boolean withinRecordedRegion(BlockPos position, BlockPos center,
			int radius, int verticalRadius) {
		int dx = position.getX() - center.getX();
		int dz = position.getZ() - center.getZ();
		return dx * dx + dz * dz <= radius * radius
				&& Math.abs(position.getY() - center.getY()) <= verticalRadius;
	}

	private static Vec3 edgePoint(BlockPos block, Direction face, RandomSource random) {
		double perimeter = random.nextDouble() * 4.0D;
		int side = (int) perimeter;
		double along = perimeter - side;
		double u = switch (side) {
			case 0 -> along;
			case 1 -> 1.0D;
			case 2 -> 1.0D - along;
			default -> 0.0D;
		};
		double v = switch (side) {
			case 0 -> 0.0D;
			case 1 -> along;
			case 2 -> 1.0D;
			default -> 1.0D - along;
		};
		double x = block.getX();
		double y = block.getY();
		double z = block.getZ();
		return switch (face) {
			case DOWN -> new Vec3(x + u, y - FACE_OFFSET, z + v);
			case UP -> new Vec3(x + u, y + 1.0D + FACE_OFFSET, z + v);
			case NORTH -> new Vec3(x + u, y + v, z - FACE_OFFSET);
			case SOUTH -> new Vec3(x + u, y + v, z + 1.0D + FACE_OFFSET);
			case WEST -> new Vec3(x - FACE_OFFSET, y + u, z + v);
			case EAST -> new Vec3(x + 1.0D + FACE_OFFSET, y + u, z + v);
		};
	}
}
