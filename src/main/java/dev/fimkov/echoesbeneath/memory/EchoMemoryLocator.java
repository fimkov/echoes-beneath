package dev.fimkov.echoesbeneath.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class EchoMemoryLocator {
	private EchoMemoryLocator() {
	}

	public static Optional<EchoMemoryReference> resonating(EchoMemoryReferences references,
			ResourceKey<Level> dimension, BlockPos position) {
		EchoMemoryReference best = null;

		for (EchoMemoryReference candidate : references.entries()) {
			if (!candidate.covers(dimension, position)) {
				continue;
			}

			if (best == null || wins(candidate, best, position)) {
				best = candidate;
			}
		}

		return Optional.ofNullable(best);
	}

	// A small memory recorded inside a large one is the one you meant when you are
	// standing in it, however the two centres happen to fall, so the tighter region
	// wins before distance is even asked.
	private static boolean wins(EchoMemoryReference candidate, EchoMemoryReference best,
			BlockPos position) {
		if (candidate.radius() != best.radius()) {
			return candidate.radius() < best.radius();
		}

		if (candidate.verticalRadius() != best.verticalRadius()) {
			return candidate.verticalRadius() < best.verticalRadius();
		}
		double distance = candidate.distanceSquaredTo(position);
		double bestDistance = best.distanceSquaredTo(position);

		if (distance != bestDistance) {
			return distance < bestDistance;
		}

		return candidate.slot() < best.slot();
	}
}
