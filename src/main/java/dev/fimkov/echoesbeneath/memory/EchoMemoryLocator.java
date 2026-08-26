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
		double bestDistance = Double.MAX_VALUE;

		for (EchoMemoryReference candidate : references.entries()) {
			if (!candidate.covers(dimension, position)) {
				continue;
			}
			double distance = candidate.distanceSquaredTo(position);
			boolean closer = distance < bestDistance;
			boolean tieBrokenBySlot = distance == bestDistance && best != null && candidate.slot() < best.slot();

			if (best == null || closer || tieBrokenBySlot) {
				best = candidate;
				bestDistance = distance;
			}
		}

		return Optional.ofNullable(best);
	}
}
