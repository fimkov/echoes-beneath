package dev.fimkov.echoesbeneath.client.screen;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.menu.ResonanceTableLayout;
import dev.fimkov.echoesbeneath.menu.ResonanceTableMenu;
import dev.fimkov.echoesbeneath.menu.ResonanceTablePlan;
import dev.fimkov.echoesbeneath.menu.ResonanceTableUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ResonanceTableScreen extends AbstractContainerScreen<ResonanceTableMenu> {
	private static final ResourceLocation TEXTURE = EchoesBeneath.id("textures/gui/resonance_table.png");

	private static final int ROW_HOVER_U = 0;
	private static final int ROW_HOVER_V = 226;
	private static final int MINUS_U = 176;
	private static final int PLUS_U = 188;
	private static final int STATE_NORMAL = 0;
	private static final int STATE_HOVERED = 12;
	private static final int STATE_DISABLED = 24;
	private static final int PIP_OWNED_U = 200;
	private static final int PIP_REMEMBERED_U = 204;
	private static final int PIP_EMPTY_U = 208;
	private static final int PIP_PAID_V = 6;
	private static final int GLOW_U = 176;
	private static final int GLOW_V = 36;
	private static final int GHOST_U = 208;
	private static final int GHOST_V = 36;

	private static final int INK = 0xB4C4C2;
	private static final int DIM = 0x68767A;
	private static final int TITLE = 0xC8DCDA;
	private static final int COST_OK = 0x7EE36B;
	private static final int COST_BAD = 0xE0605E;

	private static final float PULSE_PERIOD_TICKS = 46.0F;
	private static final float PULSE_BASE = 0.30F;
	private static final float PULSE_SWING = 0.22F;
	private static final float PULSE_READY_BOOST = 0.26F;
	private static final float DENIED_PITCH = 0.65F;
	private static final float DENIED_VOLUME = 0.7F;
	private static final int TOOLTIP_WIDTH = 170;

	private final ItemStack amethystGhost = new ItemStack(Items.AMETHYST_SHARD);
	private float animation;

	public ResonanceTableScreen(ResonanceTableMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = ResonanceTableLayout.PANEL_WIDTH;
		this.imageHeight = ResonanceTableLayout.PANEL_HEIGHT;
	}

	@Override
	protected void init() {
		super.init();
		this.titleLabelX = ResonanceTableLayout.TITLE_X;
		this.titleLabelY = ResonanceTableLayout.TITLE_Y;
		this.inventoryLabelX = ResonanceTableLayout.INVENTORY_LABEL_X;
		this.inventoryLabelY = ResonanceTableLayout.INVENTORY_LABEL_Y;
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		animation += 1.0F;
		if (animation >= PULSE_PERIOD_TICKS) {
			animation -= PULSE_PERIOD_TICKS;
		}
	}

	private boolean over(int mouseX, int mouseY, int x, int y, int width, int height) {
		int localX = mouseX - leftPos;
		int localY = mouseY - topPos;
		return localX >= x && localX < x + width && localY >= y && localY < y + height;
	}

	private boolean overRow(int mouseX, int mouseY, int index) {
		return over(mouseX, mouseY, ResonanceTableLayout.ROW_X, ResonanceTableLayout.rowY(index),
				ResonanceTableLayout.ROW_WIDTH, ResonanceTableLayout.ROW_HEIGHT);
	}

	private boolean overButton(int mouseX, int mouseY, int index, boolean increase) {
		int x = increase ? ResonanceTableLayout.PLUS_X : ResonanceTableLayout.MINUS_X;
		return over(mouseX, mouseY, x, ResonanceTableLayout.rowY(index) + ResonanceTableLayout.BUTTON_Y_OFFSET,
				ResonanceTableLayout.BUTTON_SIZE, ResonanceTableLayout.BUTTON_SIZE);
	}

	private boolean overResult(int mouseX, int mouseY) {
		return over(mouseX, mouseY, ResonanceTableLayout.RESULT_SLOT_X, ResonanceTableLayout.RESULT_SLOT_Y,
				ResonanceTableLayout.SLOT_SIZE, ResonanceTableLayout.SLOT_SIZE);
	}

	private boolean allowed(ResonanceTableUpgrade upgrade, boolean increase) {
		return increase ? menu.canIncrease(upgrade) : menu.canDecrease(upgrade);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			for (int index = 0; index < ResonanceTableLayout.ROW_COUNT; index++) {
				ResonanceTableUpgrade upgrade = ResonanceTableUpgrade.fromIndex(index);
				for (boolean increase : new boolean[] {false, true}) {
					if (!overButton((int) mouseX, (int) mouseY, index, increase)) {
						continue;
					}
					if (allowed(upgrade, increase)) {
						press(ResonanceTableUpgrade.buttonId(index, increase));
					}
					return true;
				}
			}
			if (overResult((int) mouseX, (int) mouseY) && !menu.slots.get(ResonanceTableMenu.RESULT_SLOT)
					.getItem().isEmpty() && minecraft != null && minecraft.player != null
					&& !menu.canApply(minecraft.player)) {
				deny();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void press(int buttonId) {
		if (minecraft == null || minecraft.gameMode == null) {
			return;
		}
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	private void deny() {
		if (minecraft == null) {
			return;
		}
		minecraft.getSoundManager().play(
				SimpleSoundInstance.forUI(EchoSounds.TABLE_DENY, DENIED_PITCH, DENIED_VOLUME));
	}

	private float pulse() {
		double phase = animation / PULSE_PERIOD_TICKS * Math.PI * 2.0;
		return PULSE_BASE + PULSE_SWING * (float) ((Math.sin(phase) + 1.0) * 0.5);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		renderGhostSlot(graphics);
		renderDialGlow(graphics);
		for (int index = 0; index < ResonanceTableLayout.ROW_COUNT; index++) {
			int y = topPos + ResonanceTableLayout.rowY(index);
			if (overRow(mouseX, mouseY, index)) {
				graphics.blit(TEXTURE, leftPos + ResonanceTableLayout.ROW_X, y, ROW_HOVER_U, ROW_HOVER_V,
						ResonanceTableLayout.ROW_WIDTH, ResonanceTableLayout.ROW_HEIGHT);
			}
			graphics.blit(TEXTURE, leftPos + ResonanceTableLayout.ICON_X,
					y + ResonanceTableLayout.ICON_Y_OFFSET, ICON_U + index * ResonanceTableLayout.ICON_SIZE, 0,
					ResonanceTableLayout.ICON_SIZE, ResonanceTableLayout.ICON_SIZE);
			renderPips(graphics, index, y);
			renderButton(graphics, mouseX, mouseY, index, false);
			renderButton(graphics, mouseX, mouseY, index, true);
		}
	}

	private static final int ICON_U = 212;

	private void renderGhostSlot(GuiGraphics graphics) {
		if (!menu.amethyst().isEmpty()) {
			return;
		}
		graphics.blit(TEXTURE, leftPos + ResonanceTableLayout.AMETHYST_SLOT_X,
				topPos + ResonanceTableLayout.AMETHYST_SLOT_Y, GHOST_U, GHOST_V,
				ResonanceTableLayout.SLOT_SIZE, ResonanceTableLayout.SLOT_SIZE);
	}

	private void renderDialGlow(GuiGraphics graphics) {
		float alpha = pulse();
		if (minecraft != null && minecraft.player != null && menu.canApply(minecraft.player)) {
			alpha += PULSE_READY_BOOST;
		}
		graphics.setColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, alpha));
		graphics.blit(TEXTURE,
				leftPos + ResonanceTableLayout.DIAL_CENTRE_X - ResonanceTableLayout.DIAL_GLOW_SIZE / 2,
				topPos + ResonanceTableLayout.DIAL_CENTRE_Y - ResonanceTableLayout.DIAL_GLOW_SIZE / 2,
				GLOW_U, GLOW_V, ResonanceTableLayout.DIAL_GLOW_SIZE, ResonanceTableLayout.DIAL_GLOW_SIZE);
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private void renderPips(GuiGraphics graphics, int index, int rowScreenY) {
		ResonanceTableUpgrade upgrade = ResonanceTableUpgrade.fromIndex(index);
		int count = upgrade.maxSteps();
		boolean empty = menu.lens().isEmpty();
		int peak = empty ? 0 : upgrade.stepsOf(menu.peakUpgrades());
		int staged = empty ? 0 : upgrade.stepsOf(menu.targetUpgrades());
		int left = leftPos + ResonanceTableLayout.pipsLeft(count);
		for (int pip = 0; pip < count; pip++) {
			int u = PIP_EMPTY_U;
			int v = 0;
			if (pip < staged) {
				u = PIP_OWNED_U;
				if (pip >= peak) {
					v = PIP_PAID_V;
				}
			} else if (pip < peak) {
				u = PIP_REMEMBERED_U;
			}
			graphics.blit(TEXTURE,
					left + pip * (ResonanceTableLayout.PIP_WIDTH + ResonanceTableLayout.PIP_GAP),
					rowScreenY + ResonanceTableLayout.PIP_Y_OFFSET, u, v,
					ResonanceTableLayout.PIP_WIDTH, ResonanceTableLayout.PIP_HEIGHT);
		}
	}

	private void renderButton(GuiGraphics graphics, int mouseX, int mouseY, int index, boolean increase) {
		ResonanceTableUpgrade upgrade = ResonanceTableUpgrade.fromIndex(index);
		int state = STATE_DISABLED;
		if (allowed(upgrade, increase)) {
			state = overButton(mouseX, mouseY, index, increase) ? STATE_HOVERED : STATE_NORMAL;
		}
		int x = increase ? ResonanceTableLayout.PLUS_X : ResonanceTableLayout.MINUS_X;
		graphics.blit(TEXTURE, leftPos + x,
				topPos + ResonanceTableLayout.rowY(index) + ResonanceTableLayout.BUTTON_Y_OFFSET,
				increase ? PLUS_U : MINUS_U, state,
				ResonanceTableLayout.BUTTON_SIZE, ResonanceTableLayout.BUTTON_SIZE);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, title, titleLabelX, titleLabelY, TITLE, true);
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, INK, true);
		for (int index = 0; index < ResonanceTableLayout.ROW_COUNT; index++) {
			ResonanceTableUpgrade upgrade = ResonanceTableUpgrade.fromIndex(index);
			graphics.drawString(font, Component.translatable(upgrade.nameKey()), ResonanceTableLayout.LABEL_X,
					ResonanceTableLayout.rowY(index) + ResonanceTableLayout.LABEL_Y_OFFSET, INK, true);
		}
		renderCost(graphics);
	}

	private void renderCost(GuiGraphics graphics) {
		if (menu.lens().isEmpty()) {
			return;
		}
		ResonanceTablePlan plan = menu.plan();
		if (!plan.changed() || plan.isFree()) {
			return;
		}
		boolean affordable = minecraft != null && minecraft.player != null && menu.canApply(minecraft.player);
		int colour = affordable ? COST_OK : COST_BAD;
		drawRight(graphics, Component.literal(Integer.toString(plan.experienceLevels())),
				ResonanceTableLayout.COST_Y, colour);
		drawRight(graphics, Component.translatable(EchoTranslations.UPGRADE_LEVELS),
				ResonanceTableLayout.COST_Y + ResonanceTableLayout.COST_LINE, DIM);
		if (plan.amethystShards() <= 0) {
			return;
		}
		boolean enough = minecraft != null && minecraft.player != null
				&& (minecraft.player.getAbilities().instabuild
				|| menu.amethyst().getCount() >= plan.amethystShards());
		Component count = Component.literal(Integer.toString(plan.amethystShards()));
		int iconX = ResonanceTableLayout.COST_RIGHT - ResonanceTableLayout.SHARD_ICON_SIZE;
		graphics.renderItem(amethystGhost, iconX, ResonanceTableLayout.COST_SHARD_Y);
		graphics.drawString(font, count, iconX - 2 - font.width(count),
				ResonanceTableLayout.COST_SHARD_Y + 4, enough ? DIM : COST_BAD, true);
	}

	private void drawRight(GuiGraphics graphics, Component text, int y, int colour) {
		graphics.drawString(font, text, ResonanceTableLayout.COST_RIGHT - font.width(text), y, colour, true);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
		if (hoveredSlot != null && hoveredSlot.hasItem()) {
			return;
		}
		for (int index = 0; index < ResonanceTableLayout.ROW_COUNT; index++) {
			if (overRow(mouseX, mouseY, index)) {
				graphics.renderTooltip(font, rowTooltip(index), mouseX, mouseY);
				return;
			}
		}
	}

	private List<FormattedCharSequence> rowTooltip(int index) {
		ResonanceTableUpgrade upgrade = ResonanceTableUpgrade.fromIndex(index);
		List<FormattedCharSequence> lines = new ArrayList<>();
		lines.add(Component.translatable(upgrade.nameKey()).withStyle(ChatFormatting.WHITE)
				.getVisualOrderText());
		lines.addAll(font.split(Component.translatable(upgrade.descriptionKey())
				.withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH));
		lines.add(FormattedCharSequence.EMPTY);
		if (menu.lens().isEmpty()) {
			lines.add(Component.translatable(EchoTranslations.UPGRADE_NO_LENS)
					.withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
			return lines;
		}
		int owned = upgrade.stepsOf(menu.currentUpgrades());
		int staged = upgrade.stepsOf(menu.targetUpgrades());
		int reached = upgrade.stepsOf(menu.peakUpgrades());
		Component now = Component.translatable(upgrade.unitKey(), upgrade.displayValue(owned));
		lines.add((staged == owned
				? now
				: Component.translatable(EchoTranslations.UPGRADE_CHANGE, now,
						Component.translatable(upgrade.unitKey(), upgrade.displayValue(staged))))
				.copy().withStyle(ChatFormatting.AQUA).getVisualOrderText());
		if (reached > owned) {
			lines.add(Component.translatable(EchoTranslations.UPGRADE_REMEMBERED,
							upgrade.displayValue(reached))
					.withStyle(ChatFormatting.DARK_AQUA).getVisualOrderText());
		}
		if (staged >= upgrade.maxSteps()) {
			lines.add(Component.translatable(EchoTranslations.UPGRADE_MAXED)
					.withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
			return lines;
		}
		lines.add(Component.translatable(EchoTranslations.UPGRADE_PER_STEP_SHARDS,
						upgrade.experiencePerStep(), upgrade.shardsPerStep())
				.withStyle(ChatFormatting.GRAY).getVisualOrderText());
		return lines;
	}
}
