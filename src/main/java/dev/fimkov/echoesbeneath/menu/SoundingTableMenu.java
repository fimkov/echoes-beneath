package dev.fimkov.echoesbeneath.menu;

import dev.fimkov.echoesbeneath.chart.EchoChartData;
import dev.fimkov.echoesbeneath.chart.EchoChartPreview;
import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.item.EchoChartItem;
import dev.fimkov.echoesbeneath.item.EchoLensStacks;
import dev.fimkov.echoesbeneath.memory.EchoMemoryRecord;
import dev.fimkov.echoesbeneath.runtime.EchoServerState;
import net.minecraft.network.chat.Component;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReferences;
import dev.fimkov.echoesbeneath.registry.EchoBlocks;
import dev.fimkov.echoesbeneath.registry.EchoComponents;
import dev.fimkov.echoesbeneath.registry.EchoItems;
import dev.fimkov.echoesbeneath.registry.EchoMenus;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public class SoundingTableMenu extends AbstractContainerMenu {
	public static final int LENS_SLOT = 0;
	public static final int RESULT_SLOT = 1;
	private static final int INVENTORY_ROWS = 3;
	private static final int INVENTORY_COLUMNS = 9;
	private static final int SLOT_PITCH = 18;
	private static final float TAKE_VOLUME = 0.7F;
	private static final float TAKE_PITCH = 1.0F;

	private final Container input = new SimpleContainer(1) {
		@Override
		public void setChanged() {
			super.setChanged();
			slotsChanged(this);
		}
	};
	private final Container output = new SimpleContainer(1);
	private final ContainerLevelAccess access;
	private final Player owner;
	private final DataSlot selected = DataSlot.standalone();
	private final DataSlot lowest = DataSlot.standalone();
	private final DataSlot highest = DataSlot.standalone();
	private final DataSlot worldBottom = DataSlot.standalone();
	private final DataSlot worldTop = DataSlot.standalone();
	private final DataSlot floor = DataSlot.standalone();
	private final DataSlot ceiling = DataSlot.standalone();
	private ItemStack seededFrom = ItemStack.EMPTY;

	public SoundingTableMenu(int containerId, Inventory inventory, BlockPos position) {
		this(containerId, inventory, ContainerLevelAccess.create(inventory.player.level(), position));
	}

	public SoundingTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(EchoMenus.SOUNDING_TABLE, containerId);
		this.access = access;
		this.owner = inventory.player;
		addSlot(new Slot(input, LENS_SLOT, SoundingTableLayout.LENS_SLOT_X, SoundingTableLayout.LENS_SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return EchoLensStacks.isLens(stack);
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		addSlot(new Slot(output, 0, SoundingTableLayout.RESULT_SLOT_X, SoundingTableLayout.RESULT_SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.getItem() instanceof EchoChartItem
						&& stack.get(EchoComponents.CHART_DATA) != null;
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}

			@Override
			public void onTake(Player player, ItemStack stack) {
				access.execute((level, position) -> level.playSound(null, position,
						EchoSounds.TABLE_TUNE, SoundSource.BLOCKS, TAKE_VOLUME, TAKE_PITCH));
				super.onTake(player, stack);
			}
		});
		for (int row = 0; row < INVENTORY_ROWS; row++) {
			for (int column = 0; column < INVENTORY_COLUMNS; column++) {
				addSlot(new Slot(inventory, column + row * INVENTORY_COLUMNS + INVENTORY_COLUMNS,
						SoundingTableLayout.INVENTORY_X + column * SLOT_PITCH,
						SoundingTableLayout.INVENTORY_Y + row * SLOT_PITCH));
			}
		}
		for (int column = 0; column < INVENTORY_COLUMNS; column++) {
			addSlot(new Slot(inventory, column,
					SoundingTableLayout.INVENTORY_X + column * SLOT_PITCH, SoundingTableLayout.HOTBAR_Y));
		}
		addDataSlot(selected);
		addDataSlot(lowest);
		addDataSlot(highest);
		addDataSlot(worldBottom);
		addDataSlot(worldTop);
		addDataSlot(floor);
		addDataSlot(ceiling);
		selected.set(-1);
		access.execute((level, position) -> {
			worldBottom.set(level.getMinBuildHeight());
			worldTop.set(level.getMaxBuildHeight() - 1);
			floor.set(worldBottom.get());
			ceiling.set(worldTop.get());
		});
	}

	public ItemStack lens() {
		return input.getItem(LENS_SLOT);
	}

	public List<EchoMemoryReference> memories() {
		return EchoLensStacks.references(lens()).entries();
	}

	public int selectedSlot() {
		return selected.get();
	}

	public int lowestY() {
		return lowest.get();
	}

	public int highestY() {
		return highest.get();
	}

	public int worldBottom() {
		return worldBottom.get();
	}

	public int worldTop() {
		return worldTop.get();
	}

	public int floor() {
		return floor.get();
	}

	public int ceiling() {
		return ceiling.get();
	}

	@Override
	public void slotsChanged(Container container) {
		super.slotsChanged(container);
		ItemStack lens = lens();
		if (ItemStack.matches(lens, seededFrom)) {
			return;
		}
		seededFrom = lens.copy();
		output.setItem(0, ItemStack.EMPTY);
		List<EchoMemoryReference> memories = memories();
		selected.set(memories.isEmpty() ? -1 : 0);
		resetWindow();
		pushPreview();
	}

	private void resetWindow() {
		EchoMemoryReference chosen = chosen();
		if (chosen == null) {
			floor.set(worldBottom.get());
			ceiling.set(worldTop.get());
		} else {
			floor.set(Math.max(worldBottom.get(), chosen.center().getY() - chosen.verticalRadius()));
			ceiling.set(Math.min(worldTop.get(), chosen.center().getY() + chosen.verticalRadius()));
		}
		lowest.set(floor.get());
		highest.set(ceiling.get());
	}

	private EchoMemoryReference chosen() {
		List<EchoMemoryReference> memories = memories();
		int index = selected.get();
		return index < 0 || index >= memories.size() ? null : memories.get(index);
	}

	@Override
	public boolean clickMenuButton(Player player, int button) {
		List<EchoMemoryReference> memories = memories();
		if (SoundingTablePlan.isSelect(button, memories.size())) {
			selected.set(SoundingTablePlan.offsetOf(button, SoundingTablePlan.SELECT_BASE));
			output.setItem(0, ItemStack.EMPTY);
			resetWindow();
			pushPreview();
			return true;
		}
		if (SoundingTablePlan.isLowest(button)) {
			int value = floor.get() + SoundingTablePlan.offsetOf(button, SoundingTablePlan.LOWEST_BASE);
			lowest.set(Math.clamp(value, floor.get(), highest.get()));
			output.setItem(0, ItemStack.EMPTY);
			pushPreview();
			return true;
		}
		if (SoundingTablePlan.isHighest(button)) {
			int value = floor.get() + SoundingTablePlan.offsetOf(button, SoundingTablePlan.HIGHEST_BASE);
			highest.set(Math.clamp(value, lowest.get(), ceiling.get()));
			output.setItem(0, ItemStack.EMPTY);
			pushPreview();
			return true;
		}
		if (button == SoundingTablePlan.EXTRACT) {
			return canReturn() ? putBack(player) : extract(player);
		}
		return false;
	}

	private void pushPreview() {
		if (owner instanceof ServerPlayer viewer) {
			EchoChartPreview.send(viewer, chosen(), lowest.get(), highest.get());
		}
	}

	public ItemStack chart() {
		return output.getItem(0);
	}

	public boolean canExtract() {
		return chosen() != null && output.getItem(0).isEmpty();
	}

	public boolean canReturn() {
		EchoChartData data = chart().get(EchoComponents.CHART_DATA);
		return data != null && !lens().isEmpty()
				&& EchoLensStacks.references(lens())
						.nextFreeSlot(EchoLensStacks.upgrades(lens()).memorySlots()).isPresent();
	}

	private boolean extract(Player player) {
		EchoMemoryReference chosen = chosen();
		if (chosen == null || !output.getItem(0).isEmpty() || !(player instanceof ServerPlayer viewer)) {
			return false;
		}
		int sounded = EchoChartPreview.soundedCount(viewer, chosen, lowest.get(), highest.get());
		if (sounded <= 0) {
			return false;
		}
		ItemStack lens = lens();
		EchoMemoryReferences remaining = EchoLensStacks.references(lens).withRemoved(chosen.memoryId());
		lens.set(EchoComponents.MEMORY_REFERENCES, remaining);
		seededFrom = lens.copy();
		ItemStack chart = new ItemStack(EchoItems.ECHO_CHART);
		chart.set(EchoComponents.CHART_DATA, new EchoChartData(chosen.memoryId(), chosen.dimension(),
				chosen.center(), chosen.radius(), lowest.get(), highest.get(), sounded));
		output.setItem(0, chart);
		selected.set(remaining.entries().isEmpty() ? -1 : 0);
		resetWindow();
		pushPreview();
		broadcastChanges();
		return true;
	}

	private boolean putBack(Player player) {
		EchoChartData data = chart().get(EchoComponents.CHART_DATA);
		if (data == null || !(player instanceof ServerPlayer viewer)) {
			return false;
		}
		ItemStack lens = lens();
		EchoMemoryReferences held = EchoLensStacks.references(lens);
		Optional<Integer> free = held.nextFreeSlot(EchoLensStacks.upgrades(lens).memorySlots());
		if (free.isEmpty()) {
			return false;
		}
		Optional<EchoMemoryRecord> record = EchoServerState.current() == null
				? Optional.empty()
				: EchoServerState.current().storage().load(data.memoryId());
		if (record.isEmpty()) {
			viewer.displayClientMessage(
					Component.translatable(EchoTranslations.MESSAGE_MEMORY_CORRUPT), true);
			return false;
		}
		EchoMemoryRecord kept = record.get();
		lens.set(EchoComponents.MEMORY_REFERENCES, held.withAdded(new EchoMemoryReference(
				kept.id(), free.get(), kept.dimension(), kept.center(),
				kept.radius(), kept.verticalRadius(), kept.durationTicks(),
				kept.createdAtEpochMillis())));
		seededFrom = lens.copy();
		output.setItem(0, ItemStack.EMPTY);
		selected.set(free.get());
		resetWindow();
		pushPreview();
		access.execute((level, position) -> level.playSound(null, position,
				EchoSounds.TABLE_TUNE, SoundSource.BLOCKS, TAKE_VOLUME, TAKE_PITCH));
		broadcastChanges();
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < 2) {
			if (!moveItemStackTo(stack, 2, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, 0, 1, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return copy;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		access.execute((level, position) -> {
			clearContainer(player, input);
			clearContainer(player, output);
		});
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, EchoBlocks.SOUNDING_TABLE);
	}
}
