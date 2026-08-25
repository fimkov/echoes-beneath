package dev.fimkov.echoesbeneath.registry;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.menu.ResonanceTableMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public final class EchoMenus {
	public static final MenuType<ResonanceTableMenu> RESONANCE_TABLE =
			new ExtendedScreenHandlerType<>(ResonanceTableMenu::new, BlockPos.STREAM_CODEC);

	private EchoMenus() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.MENU, EchoesBeneath.id("resonance_table"), RESONANCE_TABLE);
	}
}
