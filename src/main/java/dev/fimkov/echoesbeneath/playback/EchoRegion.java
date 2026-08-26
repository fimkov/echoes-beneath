package dev.fimkov.echoesbeneath.playback;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record EchoRegion(ResourceKey<Level> dimension, BlockPos center, int radius, int verticalRadius) {
	public AABB bounds() {
		return new AABB(
				center.getX() - radius, center.getY() - verticalRadius, center.getZ() - radius,
				center.getX() + radius + 1, center.getY() + verticalRadius + 1, center.getZ() + radius + 1);
	}

	public boolean contains(ResourceKey<Level> candidate, Vec3 position) {
		return dimension.equals(candidate) && containsPosition(position);
	}

	public boolean contains(ResourceKey<Level> candidate, BlockPos position) {
		return dimension.equals(candidate)
				&& containsPosition(new Vec3(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D));
	}

	private boolean containsPosition(Vec3 position) {
		double dx = position.x - (center.getX() + 0.5D);
		double dz = position.z - (center.getZ() + 0.5D);
		double dy = position.y - (center.getY() + 0.5D);

		return dx * dx + dz * dz <= (double) radius * radius && Math.abs(dy) <= verticalRadius + 1;
	}

	public boolean overlaps(EchoRegion other) {
		if (!dimension.equals(other.dimension)) {
			return false;
		}

		double dx = center.getX() - other.center.getX();
		double dz = center.getZ() - other.center.getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);

		if (horizontal > radius + other.radius) {
			return false;
		}

		return Math.abs(center.getY() - other.center.getY()) <= verticalRadius + other.verticalRadius;
	}

	public Vec3 centerVector() {
		return new Vec3(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D);
	}
}
