package dev.fimkov.echoesbeneath.menu;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.config.EchoConfig;
import dev.fimkov.echoesbeneath.item.EchoChartItem;
import dev.fimkov.echoesbeneath.item.EchoLensStacks;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import dev.fimkov.echoesbeneath.memory.EchoMemoryRecord;
import dev.fimkov.echoesbeneath.record.EchoRecordData;
import dev.fimkov.echoesbeneath.registry.EchoBlocks;
import dev.fimkov.echoesbeneath.registry.EchoComponents;
import dev.fimkov.echoesbeneath.registry.EchoItems;
import dev.fimkov.echoesbeneath.registry.EchoMenus;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import dev.fimkov.echoesbeneath.runtime.EchoServerState;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

public class SoundingTableMenu extends AbstractContainerMenu {
	public static final int LENS_SLOT = 0;
	public static final int RESULT_SLOT = 1;
	public static final int WAVE_BINS = 48;
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
	private final DataSlot duration = DataSlot.standalone();
	private final DataSlot start = DataSlot.standalone();
	private final DataSlot end = DataSlot.standalone();
	private final DataSlot volume = DataSlot.standalone();
	private final DataSlot playhead = DataSlot.standalone();
	private final DataSlot[] wave = new DataSlot[WAVE_BINS];
	private ItemStack seededFrom = ItemStack.EMPTY;

	public SoundingTableMenu(int containerId, Inventory inventory, BlockPos position) {
		this(containerId, inventory, ContainerLevelAccess.create(inventory.player.level(), position));
	}

	public SoundingTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(EchoMenus.SOUNDING_TABLE, containerId);
		this.access = access;
		this.owner = inventory.player;
		addSlot(new Slot(input, 0, SoundingTableLayout.LENS_SLOT_X, SoundingTableLayout.LENS_SLOT_Y) {
			@Override public boolean mayPlace(ItemStack stack) { return EchoLensStacks.isLens(stack); }
			@Override public int getMaxStackSize() { return 1; }
		});
		addSlot(new Slot(output, 0, SoundingTableLayout.RESULT_SLOT_X, SoundingTableLayout.RESULT_SLOT_Y) {
			@Override public boolean mayPlace(ItemStack stack) {
				return stack.getItem() instanceof EchoChartItem
						&& (stack.has(EchoComponents.RECORD_DATA) || stack.has(EchoComponents.CHART_DATA));
			}
			@Override public int getMaxStackSize() { return 1; }
		});
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				addSlot(new Slot(inventory, column + row * 9 + 9,
						SoundingTableLayout.INVENTORY_X + column * 18,
						SoundingTableLayout.INVENTORY_Y + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			addSlot(new Slot(inventory, column, SoundingTableLayout.INVENTORY_X + column * 18,
					SoundingTableLayout.HOTBAR_Y));
		}
		for (DataSlot value : new DataSlot[]{selected, duration, start, end, volume, playhead}) {
			addDataSlot(value);
		}
		for (int i = 0; i < WAVE_BINS; i++) {
			wave[i] = addDataSlot(DataSlot.standalone());
		}
		selected.set(-1);
		volume.set(100);
		playhead.set(-1);
	}

	public ItemStack lens() { return input.getItem(0); }
	public ItemStack chart() { return output.getItem(0); }
	public List<EchoMemoryReference> memories() { return EchoLensStacks.references(lens()).entries(); }
	public int selectedSlot() { return selected.get(); }
	public int durationTicks() { return duration.get(); }
	public int startTick() { return start.get(); }
	public int endTick() { return end.get(); }
	public int volume() { return volume.get(); }
	public int playhead() { return playhead.get(); }
	public int waveAt(int index) { return wave[index].get(); }

	public EchoMemoryReference chosen() {
		List<EchoMemoryReference> memories = memories();
		return selected.get() < 0 || selected.get() >= memories.size() ? null : memories.get(selected.get());
	}

	@Override
	public void slotsChanged(Container container) {
		super.slotsChanged(container);
		if (!ItemStack.matches(lens(), seededFrom)) {
			stopPreview();
			seededFrom = lens().copy();
			selected.set(memories().isEmpty() ? -1 : 0);
			resetEdit();
		}
	}

	private void resetEdit() {
		EchoMemoryReference chosen = chosen();
		duration.set(chosen == null ? 0 : chosen.durationTicks());
		start.set(0);
		end.set(duration.get());
		volume.set(100);
		for (DataSlot bin : wave) { bin.set(0); }
		EchoServerState state = EchoServerState.current();
		if (!(owner instanceof ServerPlayer) || state == null || chosen == null) { return; }
		state.storage().load(chosen.memoryId()).ifPresent(memory -> {
			var sounds = memory.soundTimeline();
			for (int i = 0; i < sounds.size(); i++) {
				if (sounds.isStopAt(i)) { continue; }
				int bin = Math.clamp(sounds.tickAt(i) * WAVE_BINS / Math.max(1, duration.get()), 0, WAVE_BINS - 1);
				wave[bin].set(Math.max(wave[bin].get(), Math.clamp(Math.round(sounds.volumeAt(i) * 50), 1, 100)));
			}
		});
	}

	@Override
	public boolean clickMenuButton(Player player, int button) {
		if (SoundingTablePlan.isSelect(button, memories().size())) {
			stopPreview(); selected.set(button); resetEdit(); broadcastChanges(); return true;
		}
		if (button == SoundingTablePlan.STOP) { stopPreview(); return true; }
		if (button == SoundingTablePlan.EXTRACT) { return canReturn() ? putBack(player) : extract(player); }
		if (chosen() == null || duration.get() <= 0) { return false; }
		if (button == SoundingTablePlan.PREVIEW && player instanceof ServerPlayer server) {
			EchoServerState state = EchoServerState.current();
			return state != null && state.records().preview(server, edit());
		}
		stopPreview();
		if (button >= SoundingTablePlan.START_BASE && button <= SoundingTablePlan.START_BASE + EchoRecordData.MAX_TICKS) {
			start.set(Math.clamp(button - SoundingTablePlan.START_BASE, 0, Math.max(0, end.get() - 1)));
		} else if (button >= SoundingTablePlan.END_BASE && button <= SoundingTablePlan.END_BASE + EchoRecordData.MAX_TICKS) {
			end.set(Math.clamp(button - SoundingTablePlan.END_BASE, start.get() + 1, duration.get()));
		} else if (button >= SoundingTablePlan.VOLUME_BASE && button <= SoundingTablePlan.VOLUME_BASE + EchoRecordData.MAX_VOLUME) {
			volume.set(button - SoundingTablePlan.VOLUME_BASE);
		} else { return false; }
		broadcastChanges(); return true;
	}

	private EchoRecordData edit() {
		return EchoRecordData.edited(chosen().memoryId(), duration.get(), start.get(), end.get(), volume.get());
	}

	public boolean canExtract() {
		return EchoConfig.values().allowCharts() && chosen() != null && duration.get() > 0 && chart().isEmpty();
	}

	public boolean canReturn() {
		return !chart().isEmpty() && !lens().isEmpty() && (chart().has(EchoComponents.RECORD_DATA)
				|| chart().has(EchoComponents.CHART_DATA)) && EchoLensStacks.references(lens())
				.nextFreeSlot(EchoLensStacks.effectiveUpgrades(lens()).memorySlots()).isPresent();
	}

	private boolean extract(Player player) {
		EchoServerState state = EchoServerState.current();
		if (!(player instanceof ServerPlayer server) || !canExtract() || state == null) { return false; }
		if (state.storage().load(chosen().memoryId()).isEmpty()) {
			server.displayClientMessage(Component.translatable(EchoTranslations.MESSAGE_MEMORY_CORRUPT), true); return false;
		}
		stopPreview();
		ItemStack result = new ItemStack(EchoItems.ECHO_CHART);
		result.set(EchoComponents.RECORD_DATA, edit());
		EchoLensStacks.setReferences(lens(), EchoLensStacks.references(lens()).withRemoved(chosen().memoryId()));
		output.setItem(0, result);
		seededFrom = lens().copy();
		selected.set(memories().isEmpty() ? -1 : 0); resetEdit();
		access.execute((level, position) -> level.playSound(null, position, EchoSounds.TABLE_TUNE,
				SoundSource.BLOCKS, 0.7F, 1.0F));
		broadcastChanges(); return true;
	}

	private boolean putBack(Player player) {
		EchoServerState state = EchoServerState.current();
		if (!(player instanceof ServerPlayer server) || !canReturn() || state == null) { return false; }
		EchoRecordData data = EchoChartItem.recordData(chart(), state.storage());
		var record = data == null ? java.util.Optional.<EchoMemoryRecord>empty() : state.storage().load(data.memoryId());
		if (record.isEmpty()) {
			server.displayClientMessage(Component.translatable(EchoTranslations.MESSAGE_MEMORY_CORRUPT), true); return false;
		}
		stopPreview();
		var memory = record.get();
		int free = EchoLensStacks.references(lens()).nextFreeSlot(EchoLensStacks.effectiveUpgrades(lens()).memorySlots()).orElseThrow();
		EchoLensStacks.setReferences(lens(), EchoLensStacks.references(lens()).withAdded(new EchoMemoryReference(
				memory.id(), free, memory.dimension(), memory.center(), memory.radius(), memory.verticalRadius(),
				memory.durationTicks(), memory.createdAtEpochMillis())));
		seededFrom = lens().copy(); output.setItem(0, ItemStack.EMPTY);
		selected.set(0); resetEdit(); broadcastChanges(); return true;
	}

	private void stopPreview() {
		if (owner instanceof ServerPlayer && EchoServerState.current() != null) {
			EchoServerState.current().records().stopPreview(owner.getUUID());
		}
		playhead.set(-1);
	}

	@Override
	public void broadcastChanges() {
		if (owner instanceof ServerPlayer && EchoServerState.current() != null) {
			playhead.set(EchoServerState.current().records().previewTick(owner.getUUID()));
		}
		super.broadcastChanges();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) { return ItemStack.EMPTY; }
		ItemStack stack = slot.getItem(); ItemStack copy = stack.copy();
		if (index < 2) {
			if (!moveItemStackTo(stack, 2, slots.size(), true)) { return ItemStack.EMPTY; }
		} else if (EchoLensStacks.isLens(stack)) {
			if (!moveItemStackTo(stack, LENS_SLOT, LENS_SLOT + 1, false)) { return ItemStack.EMPTY; }
		} else if (stack.getItem() instanceof EchoChartItem) {
			if (!moveItemStackTo(stack, RESULT_SLOT, RESULT_SLOT + 1, false)) { return ItemStack.EMPTY; }
		} else { return ItemStack.EMPTY; }
		if (stack.isEmpty()) { slot.setByPlayer(ItemStack.EMPTY); } else { slot.setChanged(); }
		return copy;
	}

	@Override
	public void removed(Player player) {
		super.removed(player); stopPreview();
		access.execute((level, position) -> { clearContainer(player, input); clearContainer(player, output); });
	}

	@Override
	public boolean stillValid(Player player) { return stillValid(access, player, EchoBlocks.SOUNDING_TABLE); }
}
