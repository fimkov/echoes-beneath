package dev.fimkov.echoesbeneath.client.screen;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import dev.fimkov.echoesbeneath.memory.EchoTextFormat;
import dev.fimkov.echoesbeneath.menu.SoundingTableLayout;
import dev.fimkov.echoesbeneath.menu.SoundingTableMenu;
import dev.fimkov.echoesbeneath.menu.SoundingTablePlan;
import dev.fimkov.echoesbeneath.record.EchoRecordData;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Inventory;

public class SoundingTableScreen extends AbstractContainerScreen<SoundingTableMenu> {
	private static final ResourceLocation TEXTURE = EchoesBeneath.id("textures/gui/sounding_table.png");
	private static final ResourceLocation SLIDER_TEXTURE = EchoesBeneath.id("textures/gui/sounding_slider.png");
	private static final ResourceLocation BUTTON_TEXTURE = EchoesBeneath.id("textures/gui/sounding_button.png");
	private static final int SLIDER_TEXTURE_WIDTH = 144;
	private static final int SLIDER_TEXTURE_HEIGHT = 64;
	private static final int SLIDER_TRACK_HEIGHT = 6;
	private static final int SLIDER_HANDLE_SIZE = 8;
	private static final int SLIDER_BOTTOM_PADDING = 2;
	private static final int INK = 0xFFB4C4C2;
	private static final int DIM = 0xFF8A9F9E;
	private static final int CYAN = 0xFF29DFEB;
	private EditSlider start;
	private EditSlider end;
	private EditSlider volume;
	private TableButton preview;
	private TableButton extract;
	private final TableButton[] rows = new TableButton[3];

	public SoundingTableScreen(SoundingTableMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = SoundingTableLayout.PANEL_WIDTH;
		imageHeight = SoundingTableLayout.PANEL_HEIGHT;
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = SoundingTableLayout.TITLE_X;
		titleLabelY = SoundingTableLayout.TITLE_Y;
		inventoryLabelX = SoundingTableLayout.INVENTORY_LABEL_X;
		inventoryLabelY = SoundingTableLayout.INVENTORY_LABEL_Y;
		for (int i = 0; i < rows.length; i++) {
			int index = i;
			rows[i] = addRenderableWidget(new TableButton(SoundingTableLayout.LIST_X, SoundingTableLayout.rowY(i),
					SoundingTableLayout.LIST_WIDTH, SoundingTableLayout.ROW_HEIGHT, Component.empty(),
					button -> send(SoundingTablePlan.selectButton(index)), true));
		}
		start = addRenderableWidget(new EditSlider(0, SoundingTableLayout.START_Y));
		end = addRenderableWidget(new EditSlider(1, SoundingTableLayout.END_Y));
		volume = addRenderableWidget(new EditSlider(2, SoundingTableLayout.VOLUME_Y));
		preview = addRenderableWidget(new TableButton(SoundingTableLayout.PREVIEW_X, SoundingTableLayout.EXTRACT_Y,
				SoundingTableLayout.ACTION_WIDTH, SoundingTableLayout.EXTRACT_HEIGHT,
				Component.translatable("record.echoes_beneath.listen"),
				button -> send(menu.playhead() >= 0 ? SoundingTablePlan.STOP : SoundingTablePlan.PREVIEW), false));
		extract = addRenderableWidget(new TableButton(SoundingTableLayout.EXTRACT_X, SoundingTableLayout.EXTRACT_Y,
				SoundingTableLayout.EXTRACT_WIDTH, SoundingTableLayout.EXTRACT_HEIGHT,
				Component.translatable("sounding.echoes_beneath.extract"),
				button -> send(SoundingTablePlan.EXTRACT), false));
		updateWidgets();
	}

	private void send(int button) {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
		}
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		updateWidgets();
	}

	private void updateWidgets() {
		List<EchoMemoryReference> memories = menu.memories();
		for (int i = 0; i < rows.length; i++) {
			rows[i].visible = i < memories.size();
			rows[i].active = i != menu.selectedSlot();
			rows[i].selected = i == menu.selectedSlot();
			if (i < memories.size()) {
				var memory = memories.get(i);
				rows[i].setMessage(Component.literal(EchoTextFormat.romanSlot(memory.slot()) + " "
						+ EchoTextFormat.clock(memory.durationTicks())));
			}
		}
		boolean chosen = menu.chosen() != null && menu.durationTicks() > 0;
		start.active = chosen; end.active = chosen; volume.active = chosen;
		start.sync(); end.sync(); volume.sync();
		preview.active = chosen;
		preview.setMessage(Component.translatable(menu.playhead() >= 0
				? "record.echoes_beneath.stop" : "record.echoes_beneath.listen"));
		extract.active = menu.canExtract() || menu.canReturn();
		extract.setMessage(Component.translatable(menu.canReturn()
				? "sounding.echoes_beneath.put_back" : "sounding.echoes_beneath.extract"));
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(TEXTURE, leftPos, topPos, 0.0F, 0.0F,
				imageWidth, imageHeight, 256, 256);
		graphics.fill(leftPos + 74, topPos + 16, leftPos + 210, topPos + 126, 0xFF111A1D);
		int x = leftPos + SoundingTableLayout.STAGE_X;
		int y = topPos + SoundingTableLayout.STAGE_Y;
		int width = SoundingTableLayout.STAGE_WIDTH;
		int height = SoundingTableLayout.STAGE_HEIGHT;
		graphics.fill(x, y, x + width, y + height, 0xFF091013);
		int middle = y + height / 2;
		graphics.fill(x + 3, middle, x + width - 3, middle + 1, 0xFF355152);
		if (menu.durationTicks() > 0) {
			for (int i = 0; i < SoundingTableMenu.WAVE_BINS; i++) {
				int tick = i * menu.durationTicks() / SoundingTableMenu.WAVE_BINS;
				int bar = Math.clamp(menu.waveAt(i) * Math.max(1, menu.volume()) * (height / 2 - 3) / 10000,
						0, height / 2 - 3);
				int left = x + 4 + i * (width - 8) / SoundingTableMenu.WAVE_BINS;
				graphics.fill(left, middle - bar, left + 1, middle + bar + 1,
						tick >= menu.startTick() && tick < menu.endTick() ? CYAN : 0xFF355152);
			}
			for (int tick : new int[]{menu.startTick(), menu.endTick()}) {
				int marker = x + 3 + tick * (width - 7) / menu.durationTicks();
				graphics.fill(marker, y + 2, marker + 1, y + height - 2, 0xFFD7F3EF);
			}
			if (menu.playhead() >= 0) {
				int marker = x + 3 + (menu.startTick() + menu.playhead()) * (width - 7) / menu.durationTicks();
				graphics.fill(marker, y + 2, marker + 1, y + height - 2, 0xFFFFC47A);
			}
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFC8DCDA, true);
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, INK, true);
		if (menu.chosen() == null) {
			Component label = Component.translatable(menu.lens().isEmpty()
					? "sounding.echoes_beneath.no_lens" : "sounding.echoes_beneath.no_memories");
			graphics.drawString(font, label, SoundingTableLayout.STAGE_X + (SoundingTableLayout.STAGE_WIDTH - font.width(label)) / 2,
					SoundingTableLayout.STAGE_Y + 8, DIM, false);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
		if (extract.isHovered()) {
			graphics.renderTooltip(font, Component.translatable(menu.canReturn()
					? "sounding.echoes_beneath.put_back_hint" : "sounding.echoes_beneath.warning"), mouseX, mouseY);
		}
	}

	private static int withAlpha(float alpha, int color) {
		return ((int) (alpha * 255) << 24) | (color & 0xFFFFFF);
	}

	private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, float u, float v,
			int width, int height, int textureWidth, int textureHeight, int tint) {
		RenderSystem.setShaderColor((tint >> 16 & 255) / 255.0F, (tint >> 8 & 255) / 255.0F,
				(tint & 255) / 255.0F, (tint >>> 24) / 255.0F);
		graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	private static String time(int tick) {
		return String.format(Locale.ROOT, "%d:%02d.%d", tick / 1200, tick / 20 % 60, tick % 20 / 2);
	}

	private final class TableButton extends Button {
		private final boolean compact;
		private boolean selected;
		private long pressedUntil;

		private TableButton(int x, int y, int width, int height, Component label, OnPress onPress, boolean compact) {
			super(leftPos + x, topPos + y, width, height, label, onPress, DEFAULT_NARRATION);
			this.compact = compact;
		}

		@Override
		public void onPress() {
			pressedUntil = Util.getMillis() + 120;
			super.onPress();
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			int state = selected ? 4 : !active ? 3 : Util.getMillis() < pressedUntil ? 2 : isHoveredOrFocused() ? 1 : 0;
			blit(graphics, BUTTON_TEXTURE, getX(), getY(), compact ? 64 : 0,
					state * getHeight(), getWidth(), getHeight(), 128, 128, withAlpha(alpha, 0xFFFFFF));
			int ink = selected || active && isHoveredOrFocused() ? 0xFFD7F3EF : active ? INK : 0xFF68767A;
			graphics.drawString(font, getMessage(), getX() + (getWidth() - font.width(getMessage())) / 2,
					getY() + (getHeight() - font.lineHeight) / 2 + (state == 2 ? 1 : 0), withAlpha(alpha, ink), true);
		}
	}

	private final class EditSlider extends AbstractSliderButton {
		private final int kind;
		private boolean held;

		private EditSlider(int kind, int y) {
			super(leftPos + SoundingTableLayout.STAGE_X, topPos + y,
					SoundingTableLayout.STAGE_WIDTH, SoundingTableLayout.SLIDER_HEIGHT, Component.empty(), 0.0D);
			this.kind = kind;
			sync();
		}

		private void sync() {
			if (!held) {
				int current = kind == 0 ? menu.startTick() : kind == 1 ? menu.endTick() : menu.volume();
				value = (double) current / maximum();
			}
			updateMessage();
		}

		private int maximum() { return kind == 2 ? EchoRecordData.MAX_VOLUME : Math.max(1, menu.durationTicks()); }

		@Override
		public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			int state = !active ? 3 : held ? 2 : isHoveredOrFocused() ? 1 : 0;
			int position = (int) Math.round(value * (getWidth() - SLIDER_HANDLE_SIZE));
			int trackY = getY() + getHeight() - SLIDER_TRACK_HEIGHT - SLIDER_BOTTOM_PADDING;
			int tint = withAlpha(alpha, 0xFFFFFF);
			blit(graphics, SLIDER_TEXTURE, getX(), trackY,
					0, state * SLIDER_TRACK_HEIGHT, getWidth(), SLIDER_TRACK_HEIGHT,
					SLIDER_TEXTURE_WIDTH, SLIDER_TEXTURE_HEIGHT, tint);
			if (position > 0) {
				blit(graphics, SLIDER_TEXTURE, getX() + SLIDER_HANDLE_SIZE / 2, trackY,
						SLIDER_HANDLE_SIZE / 2, (4 + state) * SLIDER_TRACK_HEIGHT, position, SLIDER_TRACK_HEIGHT,
						SLIDER_TEXTURE_WIDTH, SLIDER_TEXTURE_HEIGHT, tint);
			}
			blit(graphics, SLIDER_TEXTURE, getX() + position,
					trackY + (SLIDER_TRACK_HEIGHT - SLIDER_HANDLE_SIZE) / 2, 136, state * SLIDER_HANDLE_SIZE,
					SLIDER_HANDLE_SIZE, SLIDER_HANDLE_SIZE, SLIDER_TEXTURE_WIDTH, SLIDER_TEXTURE_HEIGHT, tint);
			int ink = active ? isHoveredOrFocused() ? 0xFFD7F3EF : INK : 0xFF68767A;
			graphics.drawString(font, getMessage(), getX() + (getWidth() - font.width(getMessage())) / 2,
					getY(), withAlpha(alpha, ink), true);
		}

		@Override
		protected void updateMessage() {
			int selected = (int) Math.round(value * maximum());
			setMessage(kind == 2 ? Component.translatable("record.echoes_beneath.volume", selected)
					: Component.translatable(kind == 0 ? "record.echoes_beneath.start" : "record.echoes_beneath.end",
							time(selected)));
		}

		@Override
		protected void applyValue() {
			int selected = (int) Math.round(value * maximum());
			send((kind == 0 ? SoundingTablePlan.START_BASE : kind == 1
					? SoundingTablePlan.END_BASE : SoundingTablePlan.VOLUME_BASE) + selected);
		}

		@Override
		public void onClick(double mouseX, double mouseY) {
			held = true;
			super.onClick(mouseX, mouseY);
		}

		@Override
		public void onRelease(double mouseX, double mouseY) {
			held = false;
			super.onRelease(mouseX, mouseY);
		}
	}
}
