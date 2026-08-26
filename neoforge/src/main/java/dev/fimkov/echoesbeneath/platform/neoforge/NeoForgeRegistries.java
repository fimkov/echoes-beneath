package dev.fimkov.echoesbeneath.platform.neoforge;

import dev.fimkov.echoesbeneath.platform.EchoRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class NeoForgeRegistries implements EchoRegistries {
	private record TabEntry(ResourceKey<CreativeModeTab> tab, Supplier<ItemStack> entry) {
	}

	private final List<TabEntry> tabEntries = new ArrayList<>();

	@Override
	public <T extends AbstractContainerMenu> MenuType<T> blockPosMenu(BlockPosMenuFactory<T> factory) {
		return IMenuTypeExtension.create((containerId, inventory, buffer) ->
				factory.create(containerId, inventory, buffer.readBlockPos()));
	}

	@Override
	public void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Supplier<ItemStack> entry) {
		tabEntries.add(new TabEntry(tab, entry));
	}

	@Override
	public void openBlockPosMenu(ServerPlayer player, MenuProvider provider, BlockPos position) {
		player.openMenu(provider, buffer -> buffer.writeBlockPos(position));
	}

	public void fillCreativeTab(BuildCreativeModeTabContentsEvent event) {
		for (TabEntry entry : tabEntries) {
			if (event.getTabKey().equals(entry.tab())) {
				event.accept(entry.entry().get());
			}
		}
	}
}
