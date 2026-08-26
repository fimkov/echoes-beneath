package dev.fimkov.echoesbeneath.registry;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.menu.ResonanceTableMenu;
import dev.fimkov.echoesbeneath.menu.SoundingTableMenu;
import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public final class EchoMenus {
	public static final MenuType<ResonanceTableMenu> RESONANCE_TABLE =
			EchoPlatforms.registries().blockPosMenu(ResonanceTableMenu::new);

	public static final MenuType<SoundingTableMenu> SOUNDING_TABLE =
			EchoPlatforms.registries().blockPosMenu(SoundingTableMenu::new);

	private EchoMenus() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.MENU, EchoesBeneath.id("resonance_table"), RESONANCE_TABLE);
		Registry.register(BuiltInRegistries.MENU, EchoesBeneath.id("sounding_table"), SOUNDING_TABLE);
	}
}
