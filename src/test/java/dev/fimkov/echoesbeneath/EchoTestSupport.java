package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class EchoTestSupport {
	public static final ResourceKey<Level> OVERWORLD =
			ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("overworld"));
	public static final ResourceKey<Level> NETHER =
			ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("the_nether"));

	public static final int DEFAULT_RADIUS = 50;
	public static final int DEFAULT_VERTICAL_RADIUS = 32;
	public static final int DEFAULT_DURATION_TICKS = 2400;

	private EchoTestSupport() {
	}

	public static EchoMemoryReference reference(int slot, ResourceKey<Level> dimension, BlockPos center) {
		return new EchoMemoryReference(UUID.randomUUID(), slot, dimension, center,
				DEFAULT_RADIUS, DEFAULT_VERTICAL_RADIUS, DEFAULT_DURATION_TICKS, 1_700_000_000_000L);
	}

	public static EchoMemoryReference reference(int slot, BlockPos center) {
		return reference(slot, OVERWORLD, center);
	}

	public static EchoMemoryReference reference(int slot, BlockPos center, int radius,
			int verticalRadius) {
		return new EchoMemoryReference(UUID.randomUUID(), slot, OVERWORLD, center,
				radius, verticalRadius, DEFAULT_DURATION_TICKS, 1_700_000_000_000L);
	}
}
