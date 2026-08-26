package dev.fimkov.echoesbeneath.platform.fabric;

import dev.fimkov.echoesbeneath.platform.EchoRegistries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public final class FabricRegistries implements EchoRegistries {
	@Override
	public <T extends AbstractContainerMenu> MenuType<T> blockPosMenu(BlockPosMenuFactory<T> factory) {
		return new ExtendedScreenHandlerType<>(factory::create, BlockPos.STREAM_CODEC);
	}

	@Override
	public void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Supplier<ItemStack> entry) {
		ItemGroupEvents.modifyEntriesEvent(tab).register(entries -> entries.accept(entry.get()));
	}

	@Override
	public void openBlockPosMenu(ServerPlayer player, MenuProvider provider, BlockPos position) {
		player.openMenu(new ExtendedScreenHandlerFactory<BlockPos>() {
			@Override
			public BlockPos getScreenOpeningData(ServerPlayer viewer) {
				return position;
			}

			@Override
			public Component getDisplayName() {
				return provider.getDisplayName();
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player viewer) {
				return provider.createMenu(containerId, inventory, viewer);
			}
		});
	}
}
