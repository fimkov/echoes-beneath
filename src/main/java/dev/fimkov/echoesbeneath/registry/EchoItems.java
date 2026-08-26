package dev.fimkov.echoesbeneath.registry;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.item.EchoLensItem;
import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class EchoItems {
	public static final EchoLensItem ECHO_LENS = new EchoLensItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));

	private EchoItems() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, EchoesBeneath.id("echo_lens"), ECHO_LENS);
		EchoPlatforms.registries().addToCreativeTab(CreativeModeTabs.TOOLS_AND_UTILITIES,
				() -> new ItemStack(ECHO_LENS));
	}
}
