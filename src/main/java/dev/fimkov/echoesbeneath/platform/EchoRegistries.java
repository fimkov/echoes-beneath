package dev.fimkov.echoesbeneath.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public interface EchoRegistries {
	@FunctionalInterface
	interface BlockPosMenuFactory<T extends AbstractContainerMenu> {
		T create(int containerId, Inventory inventory, BlockPos position);
	}

	<T extends AbstractContainerMenu> MenuType<T> blockPosMenu(BlockPosMenuFactory<T> factory);

	void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Supplier<ItemStack> entry);

	void openBlockPosMenu(ServerPlayer player, MenuProvider provider, BlockPos position);
}
