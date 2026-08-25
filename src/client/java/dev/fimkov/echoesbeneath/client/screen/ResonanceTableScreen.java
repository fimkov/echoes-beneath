package dev.fimkov.echoesbeneath.client.screen;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.menu.ResonanceTableLayout;
import dev.fimkov.echoesbeneath.menu.ResonanceTableMenu;
import dev.fimkov.echoesbeneath.menu.ResonanceTablePlan;
import dev.fimkov.echoesbeneath.menu.ResonanceTableUpgrade;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

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
	private static final int PIP_FILLED_U = 200;
	private static final int PIP_PENDING_U = 204;
	private static final int PIP_EMPTY_U = 208;
	private static final int ICON_U = 212;
	private static final int SHARD_U = 244;

	private static final int INK = 0xB4C4C2;
	private static final int DIM = 0x68767A;
	private static final int TITLE = 0xC8DCDA;
	private static final int COST_OK = 0x7EE36B;
	private static final int COST_BAD = 0xE0605E;

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
					if (!allowed(upgrade, increase)) {
						return true;
					}
					press(ResonanceTableUpgrade.buttonId(index, increase));
					return true;
				}
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

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
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

	private void renderPips(GuiGraphics graphics, int index, int rowScreenY) {
		ResonanceTableUpgrade upgrade = ResonanceTableUpgrade.fromIndex(index);
		int count = upgrade.maxSteps();
		int owned = upgrade.stepsOf(menu.floorUpgrades());
		int staged = upgrade.stepsOf(menu.targetUpgrades());
		boolean empty = menu.lens().isEmpty();
		int left = leftPos + ResonanceTableLayout.pipsLeft(count);
		for (int pip = 0; pip < count; pip++) {
			int u = PIP_EMPTY_U;
			if (!empty && pip < owned) {
				u = PIP_FILLED_U;
			} else if (!empty && pip < staged) {
				u = PIP_PENDING_U;
			}
			graphics.blit(TEXTURE,
					left + pip * (ResonanceTableLayout.PIP_WIDTH + ResonanceTableLayout.PIP_GAP),
					rowScreenY + ResonanceTableLayout.PIP_Y_OFFSET, u, 0,
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
		if (plan.isEmpty()) {
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
		graphics.blit(TEXTURE, ResonanceTableLayout.COST_RIGHT - ResonanceTableLayout.SHARD_ICON_SIZE,
				ResonanceTableLayout.COST_SHARD_Y, SHARD_U, 0,
				ResonanceTableLayout.SHARD_ICON_SIZE, ResonanceTableLayout.SHARD_ICON_SIZE);
		Component count = Component.literal(Integer.toString(plan.amethystShards()));
		graphics.drawString(font, count,
				ResonanceTableLayout.COST_RIGHT - ResonanceTableLayout.SHARD_ICON_SIZE - 2 - font.width(count),
				ResonanceTableLayout.COST_SHARD_Y, enough ? DIM : COST_BAD, true);
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
				graphics.renderComponentTooltip(font, rowTooltip(index), mouseX, mouseY);
				return;
			}
		}
	}

	private List<Component> rowTooltip(int index) {
		ResonanceTableUpgrade upgrade = ResonanceTableUpgrade.fromIndex(index);
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable(upgrade.nameKey()));
		if (menu.lens().isEmpty()) {
			lines.add(Component.translatable(EchoTranslations.UPGRADE_NO_LENS));
			return lines;
		}
		EchoLensUpgrades floor = menu.floorUpgrades();
		EchoLensUpgrades target = menu.targetUpgrades();
		int owned = upgrade.stepsOf(floor);
		int staged = upgrade.stepsOf(target);
		Component current = Component.translatable(upgrade.unitKey(), upgrade.displayValue(owned));
		lines.add(staged == owned
				? current
				: Component.translatable(EchoTranslations.UPGRADE_CHANGE, current,
						Component.translatable(upgrade.unitKey(), upgrade.displayValue(staged))));
		if (owned >= upgrade.maxSteps()) {
			lines.add(Component.translatable(EchoTranslations.UPGRADE_MAXED));
			return lines;
		}
		lines.add(upgrade.shardsPerStep() > 0
				? Component.translatable(EchoTranslations.UPGRADE_PER_STEP_SHARDS,
						upgrade.experiencePerStep(), upgrade.shardsPerStep())
				: Component.translatable(EchoTranslations.UPGRADE_PER_STEP, upgrade.experiencePerStep()));
		return lines;
	}
}
