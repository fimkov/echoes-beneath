package dev.fimkov.echoesbeneath.item;

import dev.fimkov.echoesbeneath.memory.EchoMemoryLocator;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReferences;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.Supplier;

public final class EchoLensTooltipContext {
	private static volatile Supplier<Optional<TooltipViewer>> viewerSupplier = Optional::empty;

	private EchoLensTooltipContext() {
	}

	public static void bindViewer(Supplier<Optional<TooltipViewer>> supplier) {
		viewerSupplier = supplier;
	}

	public static Optional<EchoMemoryReference> resonatingHere(EchoMemoryReferences references) {
		if (references.isEmpty()) {
			return Optional.empty();
		}
		return viewerSupplier.get().flatMap(viewer ->
				EchoMemoryLocator.resonating(references, viewer.dimension(), viewer.position()));
	}

	public record TooltipViewer(ResourceKey<Level> dimension, BlockPos position) {
	}
}
