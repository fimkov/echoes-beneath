package dev.fimkov.echoesbeneath.registry;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

public final class EchoItemGroups {
	public static final CreativeModeTab ECHOES_BENEATH = CreativeModeTab
			.builder(CreativeModeTab.Row.TOP, 0)
			.title(Component.translatable(EchoTranslations.ITEM_GROUP))
			.icon(() -> new ItemStack(EchoItems.ECHO_LENS))
			.displayItems((parameters, output) -> contents().forEach(output::accept))
			.build();

	public static List<ItemLike> contents() {
		return List.of(
				EchoItems.ECHO_LENS,
				EchoBlocks.RESONANCE_TABLE_ITEM,
				EchoBlocks.SOUNDING_TABLE_ITEM);
	}

	private EchoItemGroups() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, EchoesBeneath.id("echoes_beneath"), ECHOES_BENEATH);
	}
}
