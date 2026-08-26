package dev.fimkov.echoesbeneath.block;

import com.mojang.serialization.MapCodec;
import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.blockentity.EchoChartBlockEntity;
import dev.fimkov.echoesbeneath.menu.ChartLookMenu;
import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class EchoChartBlock extends BaseEntityBlock {
	public static final MapCodec<EchoChartBlock> CODEC = simpleCodec(EchoChartBlock::new);
	private static final VoxelShape[] SHAPES = {
			Block.box(5.0D, 1.0D, 5.0D, 11.0D, 9.0D, 11.0D),
			Block.box(3.0D, 0.0D, 3.0D, 13.0D, 13.0D, 13.0D),
			Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)};

	public EchoChartBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
		return new EchoChartBlockEntity(position, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
			Level level, BlockState state,
			net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
		return createTickerHelper(type, dev.fimkov.echoesbeneath.registry.EchoBlockEntities.ECHO_CHART,
				level.isClientSide() ? EchoChartBlockEntity::clientTick : EchoChartBlockEntity::serverTick);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos position,
			CollisionContext context) {
		return SHAPES[sizeAt(level, position)];
	}

	private static int sizeAt(BlockGetter level, BlockPos position) {
		return level.getBlockEntity(position) instanceof EchoChartBlockEntity chart
				? Math.clamp(chart.look().size(), 0, SHAPES.length - 1)
				: dev.fimkov.echoesbeneath.chart.EchoChartLook.SIZE_MEDIUM;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos position,
			CollisionContext context) {
		return net.minecraft.world.phys.shapes.Shapes.empty();
	}

	@Override
	public void setPlacedBy(Level level, BlockPos position, BlockState state,
			@Nullable LivingEntity placer, ItemStack stack) {
		if (level.getBlockEntity(position) instanceof EchoChartBlockEntity chart) {
			chart.adopt(stack);
		}
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos position, BlockState state, Player player) {
		if (!level.isClientSide() && !player.getAbilities().instabuild
				&& level.getBlockEntity(position) instanceof EchoChartBlockEntity chart) {
			popResource(level, position, chart.asItem());
		}
		return super.playerWillDestroy(level, position, state, player);
	}

	@Override
	public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos position,
			BlockState state) {
		return level.getBlockEntity(position) instanceof EchoChartBlockEntity chart
				? chart.asItem()
				: super.getCloneItemStack(level, position, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos position,
			Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(level.getBlockEntity(position) instanceof EchoChartBlockEntity chart)) {
			return InteractionResult.PASS;
		}
		if (player.isSecondaryUseActive()) {
			if (player instanceof ServerPlayer viewer) {
				EchoPlatforms.registries().openBlockPosMenu(viewer, new LookMenuProvider(position),
						position);
			}
			return InteractionResult.CONSUME;
		}
		chart.turnOnce();
		return InteractionResult.CONSUME;
	}

	private record LookMenuProvider(BlockPos position) implements MenuProvider {
		@Override
		public Component getDisplayName() {
			return Component.translatable(EchoTranslations.CONTAINER_CHART_LOOK);
		}

		@Nullable
		@Override
		public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
			return new ChartLookMenu(containerId, inventory,
					ContainerLevelAccess.create(player.level(), position), position);
		}
	}
}
