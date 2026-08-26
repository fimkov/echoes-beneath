package dev.fimkov.echoesbeneath.menu;

import dev.fimkov.echoesbeneath.blockentity.EchoChartBlockEntity;
import dev.fimkov.echoesbeneath.chart.EchoChartLook;
import dev.fimkov.echoesbeneath.registry.EchoBlocks;
import dev.fimkov.echoesbeneath.registry.EchoMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public class ChartLookMenu extends AbstractContainerMenu {
	public static final int DRIFT_ROW = 0;
	public static final int SPIN_ROW = 1;
	public static final int MOTES_ROW = 2;
	public static final int RUNS_ROW = 3;
	public static final int SIZE_ROW = 4;

	private final ContainerLevelAccess access;
	private final BlockPos position;
	private final DataSlot drifts = DataSlot.standalone();
	private final DataSlot spin = DataSlot.standalone();
	private final DataSlot motes = DataSlot.standalone();
	private final DataSlot runs = DataSlot.standalone();
	private final DataSlot size = DataSlot.standalone();

	public ChartLookMenu(int containerId, Inventory inventory, BlockPos position) {
		this(containerId, inventory,
				ContainerLevelAccess.create(inventory.player.level(), position), position);
	}

	public ChartLookMenu(int containerId, Inventory inventory, ContainerLevelAccess access,
			BlockPos position) {
		super(EchoMenus.CHART_LOOK, containerId);
		this.access = access;
		this.position = position;
		addDataSlot(drifts);
		addDataSlot(spin);
		addDataSlot(motes);
		addDataSlot(runs);
		addDataSlot(size);
		show(readLook());
	}

	private EchoChartLook readLook() {
		EchoChartLook[] held = {EchoChartLook.DEFAULT};
		access.execute((level, at) -> {
			if (level.getBlockEntity(at) instanceof EchoChartBlockEntity chart) {
				held[0] = chart.look();
			}
		});
		return held[0];
	}

	private void show(EchoChartLook look) {
		drifts.set(look.drifts() ? 1 : 0);
		spin.set(look.spin());
		motes.set(look.motes() ? 1 : 0);
		runs.set(look.runs() ? 1 : 0);
		size.set(look.size());
	}

	public EchoChartLook look() {
		return new EchoChartLook(drifts.get() != 0, spin.get(), motes.get() != 0, runs.get() != 0,
				size.get());
	}

	public BlockPos position() {
		return position;
	}

	@Override
	public boolean clickMenuButton(Player player, int button) {
		EchoChartLook current = look();
		EchoChartLook next = switch (button) {
			case DRIFT_ROW -> current.withDrifts(!current.drifts());
			case SPIN_ROW -> current.withSpin((current.spin() + 1) % EchoChartLook.SPIN_CHOICES);
			case MOTES_ROW -> current.withMotes(!current.motes());
			case RUNS_ROW -> current.withRuns(!current.runs());
			case SIZE_ROW -> current.withSize((current.size() + 1) % EchoChartLook.SIZE_CHOICES);
			default -> null;
		};
		if (next == null) {
			return false;
		}
		show(next);
		access.execute((level, at) -> {
			if (level.getBlockEntity(at) instanceof EchoChartBlockEntity chart) {
				chart.setLook(next);
			}
		});
		broadcastChanges();
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, EchoBlocks.ECHO_CHART);
	}
}
