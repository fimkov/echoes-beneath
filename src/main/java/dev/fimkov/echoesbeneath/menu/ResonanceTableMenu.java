package dev.fimkov.echoesbeneath.menu;

import dev.fimkov.echoesbeneath.advancement.EchoCriteria;
import dev.fimkov.echoesbeneath.item.EchoLensStacks;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReferences;
import dev.fimkov.echoesbeneath.registry.EchoBlocks;
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
import net.minecraft.world.item.Items;

public class ResonanceTableMenu extends AbstractContainerMenu {
	public static final int LENS_SLOT = 0;
	public static final int AMETHYST_SLOT = 1;
	public static final int RESULT_SLOT = 2;
	public static final int CONTAINER_SLOTS = 3;

	private static final float SUCCESS_VOLUME = 0.8F;
	private static final float SUCCESS_PITCH = 1.0F;

	private final Container input = new SimpleContainer(2) {
		@Override

		public void setChanged() {
			super.setChanged();
			slotsChanged(this);
		}
	};
	private final Container output = new SimpleContainer(1);
	private final ContainerLevelAccess access;
	private final DataSlot[] targetSteps = new DataSlot[ResonanceTableUpgrade.all().length];
	private final DataSlot costLevels = DataSlot.standalone();
	private final DataSlot costShards = DataSlot.standalone();
	private ItemStack seededFrom = ItemStack.EMPTY;

	public ResonanceTableMenu(int containerId, Inventory inventory, BlockPos position) {
		this(containerId, inventory, ContainerLevelAccess.create(inventory.player.level(), position));
	}

	public ResonanceTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(EchoMenus.RESONANCE_TABLE, containerId);
		this.access = access;

		addSlot(new Slot(input, LENS_SLOT, ResonanceTableLayout.LENS_SLOT_X, ResonanceTableLayout.LENS_SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return EchoLensStacks.isLens(stack);
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});

		addSlot(new Slot(input, AMETHYST_SLOT,

				ResonanceTableLayout.AMETHYST_SLOT_X, ResonanceTableLayout.AMETHYST_SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(Items.AMETHYST_SHARD);
			}
		});

		addSlot(new Slot(output, 0, ResonanceTableLayout.RESULT_SLOT_X, ResonanceTableLayout.RESULT_SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public boolean mayPickup(Player player) {
				return canApply(player);
			}

			@Override
			public void onTake(Player player, ItemStack stack) {
				applyPlan(player);
			}
		});

		for (int row = 0; row < ResonanceTableLayout.INVENTORY_ROWS; row++) {
			for (int column = 0; column < ResonanceTableLayout.INVENTORY_COLUMNS; column++) {
				addSlot(new Slot(inventory,
						column + row * ResonanceTableLayout.INVENTORY_COLUMNS + ResonanceTableLayout.INVENTORY_COLUMNS,
						ResonanceTableLayout.INVENTORY_LEFT + column * ResonanceTableLayout.SLOT_SPACING,
						ResonanceTableLayout.INVENTORY_TOP + row * ResonanceTableLayout.SLOT_SPACING));
			}
		}

		for (int column = 0; column < ResonanceTableLayout.INVENTORY_COLUMNS; column++) {
			addSlot(new Slot(inventory, column,
					ResonanceTableLayout.INVENTORY_LEFT + column * ResonanceTableLayout.SLOT_SPACING,
					ResonanceTableLayout.HOTBAR_TOP));
		}

		for (int index = 0; index < targetSteps.length; index++) {
			targetSteps[index] = addDataSlot(DataSlot.standalone());
		}

		addDataSlot(costLevels);
		addDataSlot(costShards);
	}

	public ItemStack lens() {
		return input.getItem(LENS_SLOT);
	}

	public ItemStack amethyst() {
		return input.getItem(AMETHYST_SLOT);
	}

	public EchoLensUpgrades currentUpgrades() {
		ItemStack lens = lens();

		return lens.isEmpty() ? EchoLensUpgrades.DEFAULT : EchoLensStacks.upgrades(lens);
	}

	public EchoLensUpgrades peakUpgrades() {
		ItemStack lens = lens();

		return lens.isEmpty() ? EchoLensUpgrades.DEFAULT : EchoLensStacks.peak(lens);
	}

	public EchoLensUpgrades floorUpgrades() {
		ItemStack lens = lens();

		if (lens.isEmpty()) {
			return EchoLensUpgrades.DEFAULT;
		}

		EchoMemoryReferences sealed = EchoLensStacks.references(lens);

		return ResonanceTableUpgrade.lowestSetting(sealed.filledSlots());
	}

	public EchoLensUpgrades targetUpgrades() {
		EchoLensUpgrades result = floorUpgrades();

		for (int index = 0; index < targetSteps.length; index++) {
			ResonanceTableUpgrade upgrade = ResonanceTableUpgrade.fromIndex(index);
			result = upgrade.withSteps(result, targetSteps[index].get());
		}

		return ResonanceTableUpgrade.clampToFloor(floorUpgrades(), result);
	}

	public ResonanceTablePlan plan() {
		return new ResonanceTablePlan(targetUpgrades(), costLevels.get(), costShards.get(),
				!targetUpgrades().equals(currentUpgrades()));
	}

	public boolean canApply(Player player) {
		if (output.getItem(0).isEmpty()) {
			return false;
		}

		if (player.getAbilities().instabuild) {
			return true;
		}

		return player.experienceLevel >= costLevels.get() && amethyst().getCount() >= costShards.get();
	}

	public boolean canIncrease(ResonanceTableUpgrade upgrade) {
		return !lens().isEmpty() && upgrade.stepsOf(targetUpgrades()) < upgrade.maxSteps();
	}

	public boolean canDecrease(ResonanceTableUpgrade upgrade) {
		return !lens().isEmpty() && upgrade.stepsOf(targetUpgrades()) > upgrade.stepsOf(floorUpgrades());
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		int index = ResonanceTableUpgrade.indexOfButton(buttonId);
		ResonanceTableUpgrade upgrade = ResonanceTableUpgrade.fromIndex(index);

		if (upgrade == null || lens().isEmpty()) {
			return false;
		}

		boolean increase = ResonanceTableUpgrade.isIncrease(buttonId);

		if (increase ? !canIncrease(upgrade) : !canDecrease(upgrade)) {
			return false;
		}

		targetSteps[index].set(upgrade.stepsOf(targetUpgrades()) + (increase ? 1 : -1));
		refresh();

		return true;
	}

	@Override
	public void slotsChanged(Container container) {
		super.slotsChanged(container);

		if (container == input) {
			refresh();
		}
	}

	private void refresh() {
		ItemStack lens = lens();

		if (!ItemStack.matches(seededFrom, lens)) {
			seededFrom = lens.copy();
			EchoLensUpgrades seed = currentUpgrades();

			for (int index = 0; index < targetSteps.length; index++) {
				targetSteps[index].set(ResonanceTableUpgrade.fromIndex(index).stepsOf(seed));
			}
		}

		EchoLensUpgrades floor = floorUpgrades();

		for (int index = 0; index < targetSteps.length; index++) {
			ResonanceTableUpgrade upgrade = ResonanceTableUpgrade.fromIndex(index);
			int lowest = upgrade.stepsOf(floor);
			targetSteps[index].set(Math.clamp(targetSteps[index].get(), lowest, upgrade.maxSteps()));
		}

		ResonanceTablePlan plan = ResonanceTablePlan.of(currentUpgrades(), floor, peakUpgrades(), targetUpgrades());
		costLevels.set(plan.experienceLevels());
		costShards.set(plan.amethystShards());

		if (lens.isEmpty() || !plan.changed()) {
			output.setItem(0, ItemStack.EMPTY);
		} else {
			ItemStack preview = lens.copy();
			EchoLensStacks.setUpgrades(preview, plan.target());
			output.setItem(0, preview);
		}

		broadcastChanges();
	}

	private void applyPlan(Player player) {
		int levels = costLevels.get();
		int shards = costShards.get();
		EchoLensUpgrades applied = targetUpgrades();

		if (!player.getAbilities().instabuild) {
			player.giveExperienceLevels(-levels);
			input.removeItem(AMETHYST_SLOT, shards);
		}

		if (player instanceof ServerPlayer server) {
			EchoCriteria.LENS_TUNED.trigger(server, applied);
		}

		input.setItem(LENS_SLOT, ItemStack.EMPTY);

		for (DataSlot slot : targetSteps) {
			slot.set(0);
		}

		refresh();
		access.execute((level, position) -> level.playSound(null, position,
				EchoSounds.TABLE_TUNE, SoundSource.BLOCKS, SUCCESS_VOLUME, SUCCESS_PITCH));
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);

		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack moved = slot.getItem();
		ItemStack original = moved.copy();

		if (index == RESULT_SLOT) {
			if (!canApply(player) || !moveItemStackTo(moved, CONTAINER_SLOTS, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickCraft(moved, original);
		} else if (index < CONTAINER_SLOTS) {
			if (!moveItemStackTo(moved, CONTAINER_SLOTS, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(moved, LENS_SLOT, AMETHYST_SLOT + 1, false)) {
			return ItemStack.EMPTY;
		}

		if (moved.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}

		if (original.getCount() == moved.getCount()) {
			return ItemStack.EMPTY;
		}

		slot.onTake(player, moved);

		return original;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		output.setItem(0, ItemStack.EMPTY);
		access.execute((level, position) -> clearContainer(player, input));
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, EchoBlocks.RESONANCE_TABLE);
	}
}
