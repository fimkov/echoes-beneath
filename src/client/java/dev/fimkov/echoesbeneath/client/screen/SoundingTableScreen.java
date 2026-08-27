package dev.fimkov.echoesbeneath.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.chart.EchoChartPreview;
import dev.fimkov.echoesbeneath.client.chart.ClientChartShapes;
import dev.fimkov.echoesbeneath.client.chart.EchoChartModel;
import dev.fimkov.echoesbeneath.client.config.EchoClientConfig;
import dev.fimkov.echoesbeneath.client.render.EchoChartGeometry;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import dev.fimkov.echoesbeneath.memory.EchoTextFormat;
import dev.fimkov.echoesbeneath.menu.SoundingTableLayout;
import dev.fimkov.echoesbeneath.menu.SoundingTableMenu;
import dev.fimkov.echoesbeneath.menu.SoundingTablePlan;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class SoundingTableScreen extends AbstractContainerScreen<SoundingTableMenu> {
	private static final Identifier TEXTURE = EchoesBeneath.id("textures/gui/sounding_table.png");

	private static final int INK = 0xB4C4C2;
	private static final int DIM = 0x68767A;
	private static final int TITLE = 0xC8DCDA;
	private static final int SELECTED_INK = 0xE8FFFF;
	private static final int BAND = 0x5529DFEB;
	private static final int ROW_CHOSEN = 0xFF1B3238;
	private static final int ROW_HOVER = 0xFF232B2C;
	private static final int EDGE = 0xFF29DFEB;
	private static final int EDGE_SPENT = 0xFF2C3334;
	private static final int HANDLE_FACE = 0xFFE8FFFF;
	private static final int BUTTON_READY = 0xFF2F3637;
	private static final int BUTTON_HOVER = 0xFF3A4344;
	private static final int BUTTON_SPENT = 0xFF202526;

	private static final float SPIN_DEGREES_PER_TICK = 0.6F;
	private static final float TILT_DEGREES = 22.0F;
	private static final float STAGE_FILL = 0.72F;
	private static final int STAGE_LABEL_INSET = 4;
	private static final float TURN_PER_PIXEL = 1.6F;
	private static final float MIN_PITCH_DEGREES = -80.0F;
	private static final float MAX_PITCH_DEGREES = 80.0F;
	private static final float OVER_STAGE_Z = 320.0F;
	private static final int TICKS_BETWEEN_DRAGS = 3;
	private static final float PULSE_PERIOD_TICKS = 48.0F;
	private static final float PULSE_BASE = 0.18F;
	private static final float PULSE_SWING = 0.82F;
	private static final float ANIMATION_CYCLE_TICKS = 4608.0F;
	private static final int BRACKET_LENGTH = 7;
	private static final int EDGE_RGB = 0x29DFEB;
	private static final float DENIED_PITCH = 0.65F;
	private static final float DENIED_VOLUME = 0.7F;

	private float spin;
	private float animation;
	private float heldYaw;
	private float heldPitch = TILT_DEGREES;
	private boolean turnedByHand;
	private int dragging = -1;
	private int pendingValue = Integer.MIN_VALUE;
	private int sinceSent;
	private boolean turningStage;

	public SoundingTableScreen(SoundingTableMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = SoundingTableLayout.PANEL_WIDTH;
		this.imageHeight = SoundingTableLayout.PANEL_HEIGHT;
	}

	@Override
	protected void init() {
		super.init();
		this.titleLabelX = SoundingTableLayout.TITLE_X;
		this.titleLabelY = SoundingTableLayout.TITLE_Y;
		this.inventoryLabelX = SoundingTableLayout.INVENTORY_LABEL_X;
		this.inventoryLabelY = SoundingTableLayout.INVENTORY_LABEL_Y;
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		spin += SPIN_DEGREES_PER_TICK;

		if (spin >= 360.0F) {
			spin -= 360.0F;
		}

		animation += 1.0F;

		if (animation >= ANIMATION_CYCLE_TICKS) {
			animation = 0.0F;
		}

		if (pendingValue != Integer.MIN_VALUE && ++sinceSent >= TICKS_BETWEEN_DRAGS) {
			sendWindow(pendingValue);
			pendingValue = Integer.MIN_VALUE;
			sinceSent = 0;
		}
	}

	private float pulse(float partialTick) {
		if (!EchoClientConfig.values().tableAnimations()) {
			return PULSE_BASE + PULSE_SWING * 0.5F;
		}

		double phase = (animation + partialTick) / PULSE_PERIOD_TICKS * Math.PI * 2.0;

		return PULSE_BASE + PULSE_SWING * (float) ((Math.sin(phase) + 1.0) * 0.5);
	}

	private static int glow(float strength) {
		return (Math.round(Mth.clamp(strength, 0.0F, 1.0F) * 255) << 24) | EDGE_RGB;
	}

	private boolean over(int mouseX, int mouseY, int x, int y, int width, int height) {
		int localX = mouseX - leftPos;
		int localY = mouseY - topPos;

		return localX >= x && localX < x + width && localY >= y && localY < y + height;
	}

	private boolean overRow(int mouseX, int mouseY, int index) {
		return over(mouseX, mouseY, SoundingTableLayout.LIST_X, SoundingTableLayout.rowY(index),
				SoundingTableLayout.LIST_WIDTH, SoundingTableLayout.ROW_HEIGHT);
	}

	private boolean overExtract(int mouseX, int mouseY) {
		return over(mouseX, mouseY, SoundingTableLayout.EXTRACT_X, SoundingTableLayout.EXTRACT_Y,
				SoundingTableLayout.EXTRACT_WIDTH, SoundingTableLayout.EXTRACT_HEIGHT);
	}

	private boolean overStage(int mouseX, int mouseY) {
		return over(mouseX, mouseY, SoundingTableLayout.STAGE_X, SoundingTableLayout.STAGE_Y,
				SoundingTableLayout.STAGE_WIDTH, SoundingTableLayout.STAGE_HEIGHT);
	}

	private boolean overRuler(int mouseX, int mouseY) {
		return over(mouseX, mouseY, SoundingTableLayout.RULER_X - 3, SoundingTableLayout.RULER_Y,
				SoundingTableLayout.RULER_WIDTH + 6, SoundingTableLayout.RULER_HEIGHT);
	}

	private int rulerValue(double mouseY) {
		float span = SoundingTableLayout.RULER_HEIGHT - 1;
		float fraction = 1.0F - Mth.clamp(
				((float) mouseY - topPos - SoundingTableLayout.RULER_Y) / span, 0.0F, 1.0F);

		return Math.round(menu.floor() + fraction * (menu.ceiling() - menu.floor()));
	}

	private int rulerY(int worldY) {
		float span = Math.max(1, menu.ceiling() - menu.floor());
		float fraction = Mth.clamp((worldY - menu.floor()) / span, 0.0F, 1.0F);

		return SoundingTableLayout.RULER_Y
				+ Math.round((1.0F - fraction) * (SoundingTableLayout.RULER_HEIGHT - 1));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			List<EchoMemoryReference> memories = menu.memories();

			for (int index = 0; index < memories.size(); index++) {
				if (overRow((int) mouseX, (int) mouseY, index)) {
					press(SoundingTablePlan.selectButton(index));

					return true;
				}
			}
			if (overExtract((int) mouseX, (int) mouseY)) {
				if (menu.canExtract() || menu.canReturn()) {
					press(SoundingTablePlan.EXTRACT);
				} else {
					deny();
				}
				return true;
			}
			if (overStage((int) mouseX, (int) mouseY)) {
				turningStage = true;

				if (!turnedByHand) {
					turnedByHand = true;
					heldYaw = spin;
				}
				return true;
			}
			if (overRuler((int) mouseX, (int) mouseY)) {
				pendingValue = Integer.MIN_VALUE;
				dragging = Math.abs(rulerY(menu.lowestY()) - (mouseY - topPos))
						<= Math.abs(rulerY(menu.highestY()) - (mouseY - topPos)) ? 0 : 1;
				dragTo(mouseY);

				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (turningStage) {
			heldYaw += (float) dragX * TURN_PER_PIXEL;
			heldPitch = Mth.clamp(heldPitch + (float) dragY * TURN_PER_PIXEL,
					MIN_PITCH_DEGREES, MAX_PITCH_DEGREES);

			return true;
		}

		if (dragging >= 0) {
			dragTo(mouseY);

			return true;
		}

		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (pendingValue != Integer.MIN_VALUE) {
			sendWindow(pendingValue);
			pendingValue = Integer.MIN_VALUE;
		}

		dragging = -1;
		turningStage = false;

		return super.mouseReleased(mouseX, mouseY, button);
	}

	private void dragTo(double mouseY) {
		int value = dragging == 0
				? Math.min(rulerValue(mouseY), shownHighest())
				: Math.max(rulerValue(mouseY), shownLowest());

		if (value == (dragging == 0 ? shownLowest() : shownHighest())) {
			return;
		}

		pendingValue = value;
	}

	private int shownLowest() {
		return dragging == 0 && pendingValue != Integer.MIN_VALUE ? pendingValue : menu.lowestY();
	}

	private int shownHighest() {
		return dragging == 1 && pendingValue != Integer.MIN_VALUE ? pendingValue : menu.highestY();
	}

	private void sendWindow(int value) {
		press(dragging == 0
				? SoundingTablePlan.lowestButton(value, menu.floor())
				: SoundingTablePlan.highestButton(value, menu.floor()));
		sinceSent = 0;
	}

	private void press(int buttonId) {
		if (minecraft == null || minecraft.gameMode == null) {
			return;
		}

		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	private void deny() {
		if (minecraft != null) {
			minecraft.getSoundManager().play(
					SimpleSoundInstance.forUI(EchoSounds.TABLE_DENY, DENIED_PITCH, DENIED_VOLUME));
		}
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		renderRows(graphics, mouseX, mouseY, partialTick);
		renderRuler(graphics, partialTick);
		renderExtract(graphics, mouseX, mouseY, partialTick);
		renderStage(graphics, partialTick);
		renderStageEdge(graphics, partialTick);
		renderResultGlow(graphics, partialTick);
	}

	private void renderResultGlow(GuiGraphics graphics, float partialTick) {
		if (menu.chart().isEmpty()) {
			return;
		}

		int x = leftPos + SoundingTableLayout.RESULT_SLOT_X - 2;
		int y = topPos + SoundingTableLayout.RESULT_SLOT_Y - 2;
		int size = SoundingTableLayout.SLOT_SIZE + 4;
		int lit = glow(0.25F + pulse(partialTick) * 0.75F);
		graphics.fill(x, y, x + size, y + 1, lit);
		graphics.fill(x, y + size - 1, x + size, y + size, lit);
		graphics.fill(x, y, x + 1, y + size, lit);
		graphics.fill(x + size - 1, y, x + size, y + size, lit);
	}

	private void renderStageEdge(GuiGraphics graphics, float partialTick) {
		int x = leftPos + SoundingTableLayout.STAGE_X;
		int y = topPos + SoundingTableLayout.STAGE_Y;
		int width = SoundingTableLayout.STAGE_WIDTH;
		int height = SoundingTableLayout.STAGE_HEIGHT;
		int lit = glow(pulse(partialTick));

		for (int step = 0; step < BRACKET_LENGTH; step++) {
			graphics.fill(x + step, y, x + step + 1, y + 1, lit);
			graphics.fill(x + width - step - 1, y, x + width - step, y + 1, lit);
			graphics.fill(x + step, y + height - 1, x + step + 1, y + height, lit);
			graphics.fill(x + width - step - 1, y + height - 1, x + width - step, y + height, lit);
			graphics.fill(x, y + step, x + 1, y + step + 1, lit);
			graphics.fill(x + width - 1, y + step, x + width, y + step + 1, lit);
			graphics.fill(x, y + height - step - 1, x + 1, y + height - step, lit);
			graphics.fill(x + width - 1, y + height - step - 1, x + width, y + height - step, lit);
		}
	}

	private void renderRows(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		List<EchoMemoryReference> memories = menu.memories();

		for (int index = 0; index < memories.size(); index++) {
			boolean chosen = index == menu.selectedSlot();
			boolean hovered = overRow(mouseX, mouseY, index);

			if (!chosen && !hovered) {
				continue;
			}
			int x = leftPos + SoundingTableLayout.LIST_X;
			int y = topPos + SoundingTableLayout.rowY(index);
			int width = SoundingTableLayout.LIST_WIDTH;
			int height = SoundingTableLayout.ROW_HEIGHT;
			graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1,
					chosen ? ROW_CHOSEN : ROW_HOVER);

			if (chosen) {
				int lit = glow(0.35F + pulse(partialTick) * 0.65F);
				graphics.fill(x, y, x + width, y + 1, lit);
				graphics.fill(x, y + height - 1, x + width, y + height, lit);
				graphics.fill(x, y, x + 1, y + height, lit);
				graphics.fill(x + width - 1, y, x + width, y + height, lit);
			}
		}
	}

	private void renderRuler(GuiGraphics graphics, float partialTick) {
		if (chosen() == null) {
			return;
		}

		int x = leftPos + SoundingTableLayout.RULER_X;
		int width = SoundingTableLayout.RULER_WIDTH;
		graphics.fill(x + 1, topPos + SoundingTableLayout.RULER_Y, x + width - 1,
				topPos + SoundingTableLayout.RULER_Y + SoundingTableLayout.RULER_HEIGHT, BAND);
		int windowTop = topPos + rulerY(shownHighest());
		int windowBottom = topPos + rulerY(shownLowest());
		graphics.fill(x + 2, windowTop, x + width - 2, windowBottom + 1,
				glow(0.40F + pulse(partialTick) * 0.60F));
		handle(graphics, windowTop);
		handle(graphics, windowBottom);
	}

	private void handle(GuiGraphics graphics, int y) {
		int x = leftPos + SoundingTableLayout.RULER_X
				- (SoundingTableLayout.HANDLE_WIDTH - SoundingTableLayout.RULER_WIDTH) / 2;
		graphics.fill(x, y - 1, x + SoundingTableLayout.HANDLE_WIDTH,
				y + SoundingTableLayout.HANDLE_HEIGHT - 1, EDGE);
		graphics.fill(x + 1, y, x + SoundingTableLayout.HANDLE_WIDTH - 1, y + 1, HANDLE_FACE);
	}

	private void renderExtract(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		boolean ready = menu.canExtract() || menu.canReturn();
		boolean hovered = ready && overExtract(mouseX, mouseY);
		int x = leftPos + SoundingTableLayout.EXTRACT_X;
		int y = topPos + SoundingTableLayout.EXTRACT_Y;
		int width = SoundingTableLayout.EXTRACT_WIDTH;
		int height = SoundingTableLayout.EXTRACT_HEIGHT;
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1,
				ready ? (hovered ? BUTTON_HOVER : BUTTON_READY) : BUTTON_SPENT);
		int edge = ready ? glow(0.30F + pulse(partialTick) * 0.70F) : EDGE_SPENT;
		graphics.fill(x, y, x + width, y + 1, edge);
		graphics.fill(x, y + height - 1, x + width, y + height, edge);
		graphics.fill(x, y, x + 1, y + height, edge);
		graphics.fill(x + width - 1, y, x + width, y + height, edge);
	}

	private void renderStage(GuiGraphics graphics, float partialTick) {
		EchoChartModel model = ClientChartShapes.of(EchoChartPreview.MENU_TARGET);

		if (model == null || model.size() == 0) {
			return;
		}

		int centreX = leftPos + SoundingTableLayout.STAGE_X + SoundingTableLayout.STAGE_WIDTH / 2;
		int centreY = topPos + SoundingTableLayout.STAGE_Y + SoundingTableLayout.STAGE_HEIGHT / 2;
		graphics.enableScissor(leftPos + SoundingTableLayout.STAGE_X + 1,
				topPos + SoundingTableLayout.STAGE_Y + 1,
				leftPos + SoundingTableLayout.STAGE_X + SoundingTableLayout.STAGE_WIDTH - 1,
				topPos + SoundingTableLayout.STAGE_Y + SoundingTableLayout.STAGE_HEIGHT - 1);
		Lighting.setupFor3DItems();
		PoseStack poseStack = graphics.pose();
		poseStack.pushPose();
		poseStack.translate(centreX, centreY, 220.0F);
		float scale = SoundingTableLayout.STAGE_HEIGHT * STAGE_FILL / model.widest();
		poseStack.scale(scale, -scale, scale);
		poseStack.mulPose(Axis.XP.rotationDegrees(turnedByHand ? heldPitch : TILT_DEGREES));
		poseStack.mulPose(Axis.YP.rotationDegrees(
				turnedByHand ? heldYaw : spin + SPIN_DEGREES_PER_TICK * partialTick));
		poseStack.translate(-model.spanX() * 0.5F, -model.spanY() * 0.5F, -model.spanZ() * 0.5F);
		EchoChartGeometry.emit(model, graphics.bufferSource(), poseStack);
		graphics.flush();
		poseStack.popPose();
		Lighting.setupForFlatItems();
		graphics.disableScissor();
	}

	private EchoMemoryReference chosen() {
		List<EchoMemoryReference> memories = menu.memories();
		int index = menu.selectedSlot();

		return index < 0 || index >= memories.size() ? null : memories.get(index);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, title, titleLabelX, titleLabelY, TITLE, true);
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, INK, true);
		List<EchoMemoryReference> memories = menu.memories();

		if (menu.lens().isEmpty()) {
			centred(graphics, Component.translatable(EchoTranslations.SOUNDING_NO_LENS), DIM);
		} else if (memories.isEmpty()) {
			centred(graphics, Component.translatable(EchoTranslations.SOUNDING_NO_MEMORIES), DIM);
		}

		for (int index = 0; index < memories.size(); index++) {
			EchoMemoryReference memory = memories.get(index);
			int colour = index == menu.selectedSlot() ? SELECTED_INK : INK;
			int y = SoundingTableLayout.rowY(index) + 4;
			graphics.drawString(font, EchoTextFormat.romanSlot(memory.slot()),
					SoundingTableLayout.LIST_X + 5, y, colour, true);
			Component clock = Component.literal(EchoTextFormat.clock(memory.durationTicks()));
			graphics.drawString(font, clock,
					SoundingTableLayout.LIST_X + SoundingTableLayout.LIST_WIDTH - 5 - font.width(clock),
					y, DIM, true);
		}

		if (chosen() != null) {
			Component window = Component.translatable(EchoTranslations.SOUNDING_WINDOW,
					shownLowest(), shownHighest());
			graphics.pose().pushPose();
			graphics.pose().translate(0.0F, 0.0F, OVER_STAGE_Z);
			graphics.drawString(font, window,
					SoundingTableLayout.STAGE_X + STAGE_LABEL_INSET,
					SoundingTableLayout.STAGE_Y + SoundingTableLayout.STAGE_HEIGHT - 11, DIM, true);
			graphics.pose().popPose();
		}

		boolean ready = menu.canExtract() || menu.canReturn();
		Component extract = Component.translatable(menu.canReturn()
						? EchoTranslations.SOUNDING_PUT_BACK : EchoTranslations.SOUNDING_EXTRACT)
				.withStyle(ready ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY);
		graphics.drawString(font, extract,
				SoundingTableLayout.EXTRACT_X + (SoundingTableLayout.EXTRACT_WIDTH - font.width(extract)) / 2,
				SoundingTableLayout.EXTRACT_Y + 4, 0xFFFFFF, true);
	}

	private void centred(GuiGraphics graphics, Component text, int colour) {
		graphics.pose().pushPose();
		graphics.pose().translate(0.0F, 0.0F, OVER_STAGE_Z);
		graphics.drawString(font, text,
				SoundingTableLayout.STAGE_X + (SoundingTableLayout.STAGE_WIDTH - font.width(text)) / 2,
				SoundingTableLayout.STAGE_Y + SoundingTableLayout.STAGE_HEIGHT / 2 - 4, colour, true);
		graphics.pose().popPose();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);

		if (hoveredSlot == null && overExtract(mouseX, mouseY)) {
			graphics.renderTooltip(font, Component.translatable(menu.canReturn()
					? EchoTranslations.SOUNDING_PUT_BACK_HINT : EchoTranslations.SOUNDING_WARNING),
					mouseX, mouseY);
		}
	}
}
