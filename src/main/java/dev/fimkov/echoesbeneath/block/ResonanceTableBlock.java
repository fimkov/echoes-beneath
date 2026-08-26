package dev.fimkov.echoesbeneath.block;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.menu.ResonanceTableMenu;
import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ResonanceTableBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 13.0D, 16.0D);

	public ResonanceTableBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
			BlockPos position, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos position,
			Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer viewer) {
			EchoPlatforms.registries().openBlockPosMenu(viewer, new TableMenuProvider(position), position);
		}
		return InteractionResult.CONSUME;
	}

	private record TableMenuProvider(BlockPos position) implements MenuProvider {
		@Override
		public Component getDisplayName() {
			return Component.translatable(EchoTranslations.CONTAINER_RESONANCE_TABLE);
		}

		@Nullable
		@Override
		public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
			return new ResonanceTableMenu(containerId, inventory,
					ContainerLevelAccess.create(player.level(), position));
		}
	}
}
