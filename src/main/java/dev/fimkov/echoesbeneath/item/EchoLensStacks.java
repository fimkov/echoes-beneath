package dev.fimkov.echoesbeneath.item;

import dev.fimkov.echoesbeneath.memory.EchoMemoryReferences;
import dev.fimkov.echoesbeneath.registry.EchoComponents;
import dev.fimkov.echoesbeneath.registry.EchoItems;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class EchoLensStacks {
	private EchoLensStacks() {
	}

	public static boolean isLens(ItemStack stack) {
		return stack.is(EchoItems.ECHO_LENS);
	}

	public static EchoMemoryReferences references(ItemStack stack) {
		return stack.getOrDefault(EchoComponents.MEMORY_REFERENCES, EchoMemoryReferences.EMPTY);
	}

	public static void setReferences(ItemStack stack, EchoMemoryReferences references) {
		stack.set(EchoComponents.MEMORY_REFERENCES, references);
	}

	public static UUID identityOrAssign(ItemStack stack) {
		UUID existing = stack.get(EchoComponents.LENS_IDENTITY);
		if (existing != null) {
			return existing;
		}
		UUID assigned = UUID.randomUUID();
		stack.set(EchoComponents.LENS_IDENTITY, assigned);
		return assigned;
	}

	public static UUID identityOrNull(ItemStack stack) {
		return stack.get(EchoComponents.LENS_IDENTITY);
	}
}
