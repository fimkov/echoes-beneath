package dev.fimkov.echoesbeneath.client.screen;

import com.mojang.math.Axis;
import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.client.config.EchoClientConfig;
import dev.fimkov.echoesbeneath.menu.ResonanceTableLayout;
import dev.fimkov.echoesbeneath.menu.ResonanceTableMenu;
import dev.fimkov.echoesbeneath.menu.ResonanceTablePlan;
import dev.fimkov.echoesbeneath.menu.ResonanceTableUpgrade;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ResonanceTableScreen extends AbstractContainerScreen<ResonanceTableMenu> {
	private static final Identifier TEXTURE = EchoesBeneath.id("textures/gui/resonance_table.png");

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
	private static final float FILL_RATE = 0.28F;
	private static final float FILL_SNAP = 0.02F;
	private static final float CHARGE_RATE = 0.10F;
	private static final float RING_DEGREES_PER_TICK = 0.55F;
	private static final int WIRE_LIT = 0xFF29DFEB;
	private static final int WIRE_HEAD = 0xFFE8FFFF;
	private static final int RING_FULL_ALPHA = 255;
	private static final int RING_IDLE_ALPHA = 90;
	private static final float FEEDER_PORTION = 0.62F;
	private static final float WIRES_COMPLETE = 0.999F;
	private static final float DIAL_FILL_RATE = 0.085F;
	private static final float DIAL_DRAIN_RATE = 0.22F;
	private static final float DIAL_IDLE_ALPHA = 0.20F;

	private final ItemStack amethystGhost = new ItemStack(Items.AMETHYST_SHARD);
	private final float[] shown = new float[ResonanceTableLayout.ROW_COUNT];
	private float animation;
	private float charge;
	private float dialFill;
	private float spin;

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

		for (int index = 0; index < shown.length; index++) {
			float target = menu.lens().isEmpty() ? 0.0F
					: ResonanceTableUpgrade.fromIndex(index).stepsOf(menu.targetUpgrades());
			shown[index] += (target - shown[index]) * FILL_RATE;

			if (Math.abs(target - shown[index]) < FILL_SNAP) {
				shown[index] = target;
			}
		}

		float target = menu.slots.get(ResonanceTableMenu.RESULT_SLOT).getItem().isEmpty() ? 0.0F : 1.0F;
		charge += (target - charge) * CHARGE_RATE;

		if (Math.abs(target - charge) < FILL_SNAP) {
			charge = target;
		}

		if (charge >= WIRES_COMPLETE) {
			dialFill = Math.min(1.0F, dialFill + DIAL_FILL_RATE);
		} else {
			dialFill = Math.max(0.0F, dialFill - DIAL_DRAIN_RATE);
		}

		if (dialFill > 0.0F) {
			spin += RING_DEGREES_PER_TICK;

			if (spin >= 360.0F) {
				spin -= 360.0F;
			}
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
		if (!EchoClientConfig.values().tableAnimations()) {
			return PULSE_BASE + PULSE_SWING * 0.5F;
		}

		double phase = animation / PULSE_PERIOD_TICKS * Math.PI * 2.0;

		return PULSE_BASE + PULSE_SWING * (float) ((Math.sin(phase) + 1.0) * 0.5);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		renderGhostSlot(graphics);
		renderRing(graphics, partialTick);
		renderWires(graphics);
		renderDialGlow(graphics, partialTick);

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

	private void renderGhostSlot(GuiGraphicsExtractor graphics) {
		if (!menu.amethyst().isEmpty()) {
			return;
		}

		graphics.blit(TEXTURE, leftPos + ResonanceTableLayout.AMETHYST_SLOT_X,
				topPos + ResonanceTableLayout.AMETHYST_SLOT_Y, GHOST_U, GHOST_V,
				ResonanceTableLayout.SLOT_SIZE, ResonanceTableLayout.SLOT_SIZE);
	}

	private void renderDialGlow(GuiGraphicsExtractor graphics, float partialTick) {
		int size = ResonanceTableLayout.DIAL_GLOW_SIZE;
		int x = leftPos + ResonanceTableLayout.DIAL_CENTRE_X - size / 2;
		int y = topPos + ResonanceTableLayout.DIAL_CENTRE_Y - size / 2;
		blitDial(graphics, x, y, DIAL_IDLE_ALPHA);
		float fill = filledDial(partialTick);

		if (fill <= 0.0F) {
			return;
		}

		float alpha = pulse();

		if (minecraft != null && minecraft.player != null && menu.canApply(minecraft.player)) {
			alpha += PULSE_READY_BOOST;
		}

		int wetLine = y + Math.round(size * fill);
		graphics.enableScissor(x, y, x + size, wetLine);
		blitDial(graphics, x, y, Math.min(1.0F, alpha));
		graphics.disableScissor();
	}

	private void blitDial(GuiGraphicsExtractor graphics, int x, int y, float alpha) {
		graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
		graphics.blit(TEXTURE, x, y, GLOW_U, GLOW_V,
				ResonanceTableLayout.DIAL_GLOW_SIZE, ResonanceTableLayout.DIAL_GLOW_SIZE);
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private float filledDial(float partialTick) {
		float rate = charge >= WIRES_COMPLETE ? DIAL_FILL_RATE : -DIAL_DRAIN_RATE;

		return Mth.clamp(dialFill + rate * partialTick, 0.0F, 1.0F);
	}

	private void renderRing(GuiGraphicsExtractor graphics, float partialTick) {
		float fill = filledDial(partialTick);
		float angle = spin + (dialFill > 0.0F ? RING_DEGREES_PER_TICK * partialTick : 0.0F);
		float alpha = (RING_IDLE_ALPHA + (RING_FULL_ALPHA - RING_IDLE_ALPHA) * fill) / 255.0F;
		int half = ResonanceTableLayout.RING_SPRITE_SIZE / 2;
		float centreX = leftPos + ResonanceTableLayout.DIAL_CENTRE_X;
		float centreY = topPos + ResonanceTableLayout.DIAL_CENTRE_Y;
		graphics.pose().pushMatrix();
		graphics.pose().translate(centreX, centreY);
		graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
		graphics.pose().translate(-centreX, -centreY);
		graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
		graphics.blit(TEXTURE, (int) centreX - half, (int) centreY - half,
				ResonanceTableLayout.RING_SPRITE_U, ResonanceTableLayout.RING_SPRITE_V,
				ResonanceTableLayout.RING_SPRITE_SIZE, ResonanceTableLayout.RING_SPRITE_SIZE);
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		graphics.pose().popMatrix();
	}

	private void renderWires(GuiGraphicsExtractor graphics) {
		if (charge <= 0.0F) {
			return;
		}

		List<int[]> lens = ResonanceTableLayout.trace(ResonanceTableLayout.FEEDER_FROM_LENS);
		List<int[]> amethyst = ResonanceTableLayout.trace(ResonanceTableLayout.FEEDER_FROM_AMETHYST);
		List<int[]> trunk = ResonanceTableLayout.trace(ResonanceTableLayout.TRUNK_TO_DIAL);
		float feederShare = Math.min(1.0F, charge / FEEDER_PORTION);
		float trunkShare = Math.max(0.0F, (charge - FEEDER_PORTION) / (1.0F - FEEDER_PORTION));
		drawWire(graphics, lens, feederShare);
		drawWire(graphics, amethyst, feederShare);
		drawWire(graphics, trunk, trunkShare);
	}

	private void drawWire(GuiGraphicsExtractor graphics, List<int[]> path, float share) {
		int lit = Math.round(Mth.clamp(share, 0.0F, 1.0F) * path.size());

		for (int index = 0; index < lit; index++) {
			int[] point = path.get(index);
			int x = leftPos + point[0];
			int y = topPos + point[1];
			graphics.fill(x, y, x + 1, y + 1, index >= lit - 2 ? WIRE_HEAD : WIRE_LIT);
		}
	}

	private void renderPips(GuiGraphicsExtractor graphics, int index, int rowScreenY) {
		ResonanceTableUpgrade upgrade = ResonanceTableUpgrade.fromIndex(index);
		int count = upgrade.maxSteps();
		boolean empty = menu.lens().isEmpty();
		int peak = empty ? 0 : upgrade.stepsOf(menu.peakUpgrades());
		int staged = empty ? 0 : upgrade.stepsOf(menu.targetUpgrades());
		float filled = empty ? 0.0F : shown[index];
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
			int x = left + pip * (ResonanceTableLayout.PIP_WIDTH + ResonanceTableLayout.PIP_GAP);
			int y = rowScreenY + ResonanceTableLayout.PIP_Y_OFFSET;
			float reached = Mth.clamp(filled - pip, 0.0F, 1.0F);

			if (u != PIP_EMPTY_U && reached < 1.0F) {
				graphics.blit(TEXTURE, x, y, PIP_EMPTY_U, 0,
						ResonanceTableLayout.PIP_WIDTH, ResonanceTableLayout.PIP_HEIGHT);

				if (reached <= 0.0F) {
					continue;
				}
				graphics.setColor(1.0F, 1.0F, 1.0F, reached);
			}
			graphics.blit(TEXTURE, x, y, u, v,
					ResonanceTableLayout.PIP_WIDTH, ResonanceTableLayout.PIP_HEIGHT);
			graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		}
	}

	private void renderButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int index, boolean increase) {
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
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, titleLabelX, titleLabelY, TITLE, true);
		graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, INK, true);

		for (int index = 0; index < ResonanceTableLayout.ROW_COUNT; index++) {
			ResonanceTableUpgrade upgrade = ResonanceTableUpgrade.fromIndex(index);
			graphics.text(font, Component.translatable(upgrade.nameKey()), ResonanceTableLayout.LABEL_X,
					ResonanceTableLayout.rowY(index) + ResonanceTableLayout.LABEL_Y_OFFSET, INK, true);
		}

		renderCost(graphics);
	}

	private void renderCost(GuiGraphicsExtractor graphics) {
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
		graphics.item(amethystGhost, iconX, ResonanceTableLayout.COST_SHARD_Y);
		graphics.text(font, count, iconX - 2 - font.width(count),
				ResonanceTableLayout.COST_SHARD_Y + 4, enough ? DIM : COST_BAD, true);
	}

	private void drawRight(GuiGraphicsExtractor graphics, Component text, int y, int colour) {
		graphics.text(font, text, ResonanceTableLayout.COST_RIGHT - font.width(text), y, colour, true);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);

		if (hoveredSlot != null && hoveredSlot.hasItem()) {
			return;
		}

		for (int index = 0; index < ResonanceTableLayout.ROW_COUNT; index++) {
			if (overRow(mouseX, mouseY, index)) {
				graphics.setTooltipForNextFrame(font, rowTooltip(index), mouseX, mouseY);

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
