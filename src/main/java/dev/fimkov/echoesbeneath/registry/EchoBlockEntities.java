package dev.fimkov.echoesbeneath.registry;

import java.util.Set;
import com.mojang.datafixers.types.Type;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.blockentity.EchoChartBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class EchoBlockEntities {
	public static final BlockEntityType<EchoChartBlockEntity> ECHO_CHART =
			new BlockEntityType<>(EchoChartBlockEntity::new, Set.of(EchoBlocks.ECHO_CHART));

	private EchoBlockEntities() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, EchoesBeneath.id("echo_chart"), ECHO_CHART);
	}
}
