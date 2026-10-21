package dev.fimkov.echoesbeneath.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.item.EchoChartItem;
import dev.fimkov.echoesbeneath.item.EchoLensItem;
import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public final class EchoItems {
	public static final EchoLensItem ECHO_LENS = new EchoLensItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
			.setId(ResourceKey.create(Registries.ITEM, EchoesBeneath.id("echo_lens"))));

	public static final EchoChartItem ECHO_CHART = new EchoChartItem(
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
					.jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, EchoesBeneath.id("echo_record")))
					.setId(ResourceKey.create(Registries.ITEM, EchoesBeneath.id("echo_chart"))));

	private EchoItems() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, EchoesBeneath.id("echo_lens"), ECHO_LENS);
		Registry.register(BuiltInRegistries.ITEM, EchoesBeneath.id("echo_chart"), ECHO_CHART);
		EchoPlatforms.registries().addToCreativeTab(CreativeModeTabs.TOOLS_AND_UTILITIES,
				() -> new ItemStack(ECHO_LENS));
	}
}
