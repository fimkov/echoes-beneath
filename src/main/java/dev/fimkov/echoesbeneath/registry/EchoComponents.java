package dev.fimkov.echoesbeneath.registry;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReferences;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.UUID;

public final class EchoComponents {
	public static final DataComponentType<UUID> LENS_IDENTITY = DataComponentType.<UUID>builder()
			.persistent(UUIDUtil.CODEC)
			.networkSynchronized(UUIDUtil.STREAM_CODEC)
			.build();

	public static final DataComponentType<EchoMemoryReferences> MEMORY_REFERENCES = DataComponentType.<EchoMemoryReferences>builder()
			.persistent(EchoMemoryReferences.CODEC)
			.networkSynchronized(EchoMemoryReferences.STREAM_CODEC)
			.build();

	public static final DataComponentType<EchoLensUpgrades> LENS_UPGRADES = DataComponentType.<EchoLensUpgrades>builder()
			.persistent(EchoLensUpgrades.CODEC)
			.networkSynchronized(EchoLensUpgrades.STREAM_CODEC)
			.build();

	private EchoComponents() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, EchoesBeneath.id("lens_identity"), LENS_IDENTITY);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, EchoesBeneath.id("memory_references"), MEMORY_REFERENCES);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, EchoesBeneath.id("lens_upgrades"), LENS_UPGRADES);
	}
}
