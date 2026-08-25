package dev.fimkov.echoesbeneath.menu;

import dev.fimkov.echoesbeneath.item.EchoLensStacks;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.registry.EchoBlocks;
import dev.fimkov.echoesbeneath.registry.EchoMenus;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ResonanceTableMenu extends AbstractContainerMenu {
	public static final int LENS_SLOT = 0;
	public static final int AMETHYST_SLOT = 1;
	private static final int CONTAINER_SLOTS = 2;
	private static final int INVENTORY_ROWS = 3;
	private static final int INVENTORY_COLUMNS = 9;
	private static final int SLOT_SPACING = 18;
	private static final int INVENTORY_LEFT = 8;
	private static final int INVENTORY_TOP = 84;
	private static final int HOTBAR_TOP = 142;
	private static final int LENS_SLOT_X = 26;
	private static final int LENS_SLOT_Y = 47;
	private static final int AMETHYST_SLOT_X = 50;
	private static final int AMETHYST_SLOT_Y = 47;
	private static final float SUCCESS_VOLUME = 0.8F;
	private static final float SUCCESS_PITCH = 1.3F;

	private final Container container = new SimpleContainer(CONTAINER_SLOTS) {
		@Override
		public void setChanged() {
			super.setChanged();
			slotsChanged(this);
		}
	};
	private final ContainerLevelAccess access;

	public ResonanceTableMenu(int containerId, Inventory inventory, BlockPos position) {
		this(containerId, inventory, ContainerLevelAccess.create(inventory.player.level(), position));
	}

	public ResonanceTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(EchoMenus.RESONANCE_TABLE, containerId);
		this.access = access;
		addSlot(new Slot(container, LENS_SLOT, LENS_SLOT_X, LENS_SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return EchoLensStacks.isLens(stack);
			}
		});
		addSlot(new Slot(container, AMETHYST_SLOT, AMETHYST_SLOT_X, AMETHYST_SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(Items.AMETHYST_SHARD);
			}
		});
		for (int row = 0; row < INVENTORY_ROWS; row++) {
			for (int column = 0; column < INVENTORY_COLUMNS; column++) {
				addSlot(new Slot(inventory, column + row * INVENTORY_COLUMNS + INVENTORY_COLUMNS,
						INVENTORY_LEFT + column * SLOT_SPACING, INVENTORY_TOP + row * SLOT_SPACING));
			}
		}
		for (int column = 0; column < INVENTORY_COLUMNS; column++) {
			addSlot(new Slot(inventory, column, INVENTORY_LEFT + column * SLOT_SPACING, HOTBAR_TOP));
		}
	}

	public ItemStack lens() {
		return container.getItem(LENS_SLOT);
	}

	public EchoLensUpgrades upgrades() {
		return EchoLensStacks.upgrades(lens());
	}

	public boolean canAfford(Player player, ResonanceTableUpgrade upgrade) {
		if (upgrade == null || lens().isEmpty() || !upgrade.isAvailable(upgrades())) {
			return false;
		}
		if (player.getAbilities().instabuild) {
			return true;
		}
		return player.experienceLevel >= upgrade.experienceLevels()
				&& container.getItem(AMETHYST_SLOT).getCount() >= upgrade.amethystShards();
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		ResonanceTableUpgrade upgrade = ResonanceTableUpgrade.fromIndex(buttonId);
		if (!canAfford(player, upgrade)) {
			return false;
		}
		ItemStack lens = lens();
		EchoLensStacks.setUpgrades(lens, upgrade.apply(EchoLensStacks.upgrades(lens)));
		if (!player.getAbilities().instabuild) {
			player.giveExperienceLevels(-upgrade.experienceLevels());
			container.removeItem(AMETHYST_SLOT, upgrade.amethystShards());
		}
		container.setChanged();
		access.execute((level, position) -> level.playSound(null, position,
				EchoSounds.LENS_RECORDING_COMPLETE, SoundSource.BLOCKS, SUCCESS_VOLUME, SUCCESS_PITCH));
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack moved = slot.getItem();
		ItemStack original = moved.copy();
		if (index < CONTAINER_SLOTS) {
			if (!moveItemStackTo(moved, CONTAINER_SLOTS, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(moved, 0, CONTAINER_SLOTS, false)) {
			return ItemStack.EMPTY;
		}
		if (moved.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return original;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		access.execute((level, position) -> clearContainer(player, container));
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, EchoBlocks.RESONANCE_TABLE);
	}
}
