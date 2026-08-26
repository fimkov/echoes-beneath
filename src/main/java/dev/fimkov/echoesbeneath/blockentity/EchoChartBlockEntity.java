package dev.fimkov.echoesbeneath.blockentity;

import dev.fimkov.echoesbeneath.chart.EchoChartData;
import dev.fimkov.echoesbeneath.chart.EchoChartLook;
import dev.fimkov.echoesbeneath.registry.EchoBlockEntities;
import dev.fimkov.echoesbeneath.registry.EchoComponents;
import dev.fimkov.echoesbeneath.registry.EchoItems;
import dev.fimkov.echoesbeneath.runtime.EchoServerState;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class EchoChartBlockEntity extends BlockEntity {
	public static final int TURN_STEPS = 16;
	private static final String TAG_CHART = "chart";
	private static final String TAG_LOOK = "look";
	private static final String TAG_TURN = "turn";
	private static final int TELL_INTERVAL_TICKS = 20;
	private static final double TELL_REACH = 80.0D;

	private EchoChartData chart;
	private EchoChartLook look = EchoChartLook.DEFAULT;
	private int turn;
	private int age;
	private final Set<ServerPlayer> told =
			Collections.newSetFromMap(new WeakHashMap<>());

	public EchoChartBlockEntity(BlockPos position, BlockState state) {
		super(EchoBlockEntities.ECHO_CHART, position, state);
	}

	@Nullable
	public EchoChartData chart() {
		return chart;
	}

	public EchoChartLook look() {
		return look;
	}

	public int turn() {
		return turn;
	}

	public float age(float partialTick) {
		return age + partialTick;
	}

	public void tickVisuals() {
		age++;
	}

	public static void serverTick(Level level, BlockPos position,
			BlockState state, EchoChartBlockEntity chart) {
		chart.age++;

		if (chart.age % TELL_INTERVAL_TICKS != 0) {
			return;
		}

		if (chart.chart == null) {
			return;
		}

		EchoServerState server =
				EchoServerState.current();

		if (server == null) {
			return;
		}

		chart.told.removeIf(ServerPlayer::hasDisconnected);

		for (Player nearby : level.players()) {
			if (!(nearby instanceof ServerPlayer viewer)
					|| chart.told.contains(viewer)
					|| viewer.distanceToSqr(position.getX() + 0.5D, position.getY() + 0.5D,
							position.getZ() + 0.5D) > TELL_REACH * TELL_REACH) {
				continue;
			}
			if (server.charts().send(viewer, position, chart.chart)) {
				chart.told.add(viewer);
			}
		}
	}

	public static void clientTick(Level level, BlockPos position,
			BlockState state, EchoChartBlockEntity chart) {
		chart.age++;
	}

	public void adopt(ItemStack stack) {
		chart = stack.get(EchoComponents.CHART_DATA);
		EchoChartLook held = stack.get(EchoComponents.CHART_LOOK);
		look = held == null ? EchoChartLook.DEFAULT : held;
		publish();
	}

	public void setLook(EchoChartLook value) {
		look = value;
		publish();
	}

	public void turnOnce() {
		turn = turn + 1 >= TURN_STEPS ? 0 : turn + 1;
		publish();
	}

	public ItemStack asItem() {
		ItemStack stack = new ItemStack(EchoItems.ECHO_CHART);

		if (chart != null) {
			stack.set(EchoComponents.CHART_DATA, chart);
		}

		stack.set(EchoComponents.CHART_LOOK, look);

		return stack;
	}

	private void publish() {
		told.clear();
		setChanged();

		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		chart = tag.contains(TAG_CHART)
				? EchoChartData.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_CHART)).result().orElse(null)
				: null;
		look = tag.contains(TAG_LOOK)
				? EchoChartLook.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_LOOK)).result()
						.orElse(EchoChartLook.DEFAULT)
				: EchoChartLook.DEFAULT;
		turn = Math.clamp(tag.getInt(TAG_TURN), 0, TURN_STEPS - 1);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);

		if (chart != null) {
			EchoChartData.CODEC.encodeStart(NbtOps.INSTANCE, chart).result()
					.ifPresent(encoded -> tag.put(TAG_CHART, encoded));
		}

		EchoChartLook.CODEC.encodeStart(NbtOps.INSTANCE, look).result()
				.ifPresent(encoded -> tag.put(TAG_LOOK, encoded));
		tag.putInt(TAG_TURN, turn);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		saveAdditional(tag, registries);

		return tag;
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
