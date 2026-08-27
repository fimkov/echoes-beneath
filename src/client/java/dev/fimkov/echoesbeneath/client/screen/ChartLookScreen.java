package dev.fimkov.echoesbeneath.client.screen;

import net.minecraft.client.input.MouseButtonEvent;
import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.chart.EchoChartLook;
import dev.fimkov.echoesbeneath.menu.ChartLookLayout;
import dev.fimkov.echoesbeneath.menu.ChartLookMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class ChartLookScreen extends AbstractContainerScreen<ChartLookMenu> {
	private static final int PANEL = 0xF00B1013;
	private static final int PANEL_EDGE = 0xFF3F4849;
	private static final int ROW = 0xFF1C2122;
	private static final int ROW_HOVER = 0xFF273031;
	private static final int VALUE_WELL = 0xFF0D1113;
	private static final int INK = 0xB4C4C2;
	private static final int TITLE = 0xC8DCDA;
	private static final int LIT = 0x29DFEB;
	private static final float PULSE_PERIOD_TICKS = 48.0F;

	private float animation;

	public ChartLookScreen(ChartLookMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = ChartLookLayout.PANEL_WIDTH;
		this.imageHeight = ChartLookLayout.PANEL_HEIGHT;
	}

	@Override
	protected void init() {
		super.init();
		this.titleLabelX = ChartLookLayout.TITLE_X;
		this.titleLabelY = ChartLookLayout.TITLE_Y;
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		animation += 1.0F;

		if (animation >= PULSE_PERIOD_TICKS) {
			animation -= PULSE_PERIOD_TICKS;
		}
	}

	private int glow(float partialTick) {
		double phase = (animation + partialTick) / PULSE_PERIOD_TICKS * Math.PI * 2.0;
		float strength = 0.45F + 0.55F * (float) ((Math.sin(phase) + 1.0) * 0.5);

		return (Math.round(Mth.clamp(strength, 0.0F, 1.0F) * 255) << 24) | LIT;
	}

	private boolean overRow(int mouseX, int mouseY, int index) {
		int localX = mouseX - leftPos;
		int localY = mouseY - topPos;

		return localX >= ChartLookLayout.ROW_X && localX < ChartLookLayout.ROW_X + ChartLookLayout.ROW_WIDTH
				&& localY >= ChartLookLayout.rowY(index)
				&& localY < ChartLookLayout.rowY(index) + ChartLookLayout.ROW_HEIGHT;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();

		if (button == 0) {
			for (int index = 0; index < ChartLookLayout.ROW_COUNT; index++) {
				if (overRow((int) mouseX, (int) mouseY, index)) {
					if (minecraft != null && minecraft.gameMode != null) {
						minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
						minecraft.getSoundManager().play(
								SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					}
					return true;
				}
			}
		}

		return super.mouseClicked(event, doubled);
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, PANEL_EDGE);
		graphics.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth,
				topPos + imageHeight, PANEL_EDGE);
		graphics.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight, PANEL_EDGE);
		graphics.fill(leftPos + imageWidth - 1, topPos, leftPos + imageWidth,
				topPos + imageHeight, PANEL_EDGE);

		for (int index = 0; index < ChartLookLayout.ROW_COUNT; index++) {
			int x = leftPos + ChartLookLayout.ROW_X;
			int y = topPos + ChartLookLayout.rowY(index);
			boolean hovered = overRow(mouseX, mouseY, index);
			graphics.fill(x, y, x + ChartLookLayout.ROW_WIDTH, y + ChartLookLayout.ROW_HEIGHT,
					hovered ? ROW_HOVER : ROW);
			int wellX = leftPos + ChartLookLayout.valueX();
			graphics.fill(wellX, y + 3, wellX + ChartLookLayout.VALUE_WIDTH,
					y + ChartLookLayout.ROW_HEIGHT - 3, VALUE_WELL);

			if (hovered) {
				int lit = glow(partialTick);
				graphics.fill(x, y, x + ChartLookLayout.ROW_WIDTH, y + 1, lit);
				graphics.fill(x, y + ChartLookLayout.ROW_HEIGHT - 1, x + ChartLookLayout.ROW_WIDTH,
						y + ChartLookLayout.ROW_HEIGHT, lit);
				graphics.fill(x, y, x + 1, y + ChartLookLayout.ROW_HEIGHT, lit);
				graphics.fill(x + ChartLookLayout.ROW_WIDTH - 1, y, x + ChartLookLayout.ROW_WIDTH,
						y + ChartLookLayout.ROW_HEIGHT, lit);
			}
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, titleLabelX, titleLabelY, TITLE, true);
		EchoChartLook look = menu.look();

		String[] names = {EchoTranslations.LOOK_DRIFT, EchoTranslations.LOOK_SPIN,
				EchoTranslations.LOOK_MOTES, EchoTranslations.LOOK_RUNS, EchoTranslations.LOOK_SIZE};

		Component[] values = {
				toggle(look.drifts()), spinName(look.spin()), toggle(look.motes()),
				toggle(look.runs()), sizeName(look.size())};

		for (int index = 0; index < ChartLookLayout.ROW_COUNT; index++) {
			int y = ChartLookLayout.rowY(index) + 5;
			graphics.text(font, Component.translatable(names[index]),
					ChartLookLayout.ROW_X + ChartLookLayout.LABEL_INSET, y, INK, true);
			int centre = ChartLookLayout.valueX() + ChartLookLayout.VALUE_WIDTH / 2;
			graphics.text(font, values[index], centre - font.width(values[index]) / 2, y,
					0xE8FFFF, true);
		}
	}

	private static Component toggle(boolean value) {
		return Component.translatable(value ? EchoTranslations.LOOK_ON : EchoTranslations.LOOK_OFF);
	}

	private static Component spinName(int spin) {
		return Component.translatable(switch (spin) {
			case EchoChartLook.SPIN_STILL -> EchoTranslations.LOOK_SPIN_STILL;
			case EchoChartLook.SPIN_BRISK -> EchoTranslations.LOOK_SPIN_BRISK;
			default -> EchoTranslations.LOOK_SPIN_SLOW;
		});
	}

	private static Component sizeName(int size) {
		return Component.translatable(switch (size) {
			case EchoChartLook.SIZE_SMALL -> EchoTranslations.LOOK_SIZE_SMALL;
			case EchoChartLook.SIZE_LARGE -> EchoTranslations.LOOK_SIZE_LARGE;
			default -> EchoTranslations.LOOK_SIZE_MEDIUM;
		});
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}
}
