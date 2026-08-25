package dev.fimkov.echoesbeneath.registry;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class EchoItemGroups {
	public static final CreativeModeTab ECHOES_BENEATH = CreativeModeTab
			.builder(CreativeModeTab.Row.TOP, 0)
			.title(Component.translatable(EchoTranslations.ITEM_GROUP))
			.icon(() -> new ItemStack(EchoItems.ECHO_LENS))
			.displayItems((parameters, output) -> {
				output.accept(EchoItems.ECHO_LENS);
				output.accept(EchoBlocks.RESONANCE_TABLE_ITEM);
			})
			.build();

	private EchoItemGroups() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, EchoesBeneath.id("echoes_beneath"), ECHOES_BENEATH);
	}
}
