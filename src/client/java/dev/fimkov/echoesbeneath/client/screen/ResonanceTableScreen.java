package dev.fimkov.echoesbeneath.client.screen;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.menu.ResonanceTableMenu;
import dev.fimkov.echoesbeneath.menu.ResonanceTableUpgrade;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ResonanceTableScreen extends AbstractContainerScreen<ResonanceTableMenu> {
	private static final ResourceLocation BACKGROUND = EchoesBeneath.id("textures/gui/resonance_table.png");
	private static final int PANEL_WIDTH = 176;
	private static final int PANEL_HEIGHT = 166;
	private static final int BUTTON_LEFT = 74;
	private static final int BUTTON_TOP = 18;
	private static final int BUTTON_WIDTH = 94;
	private static final int BUTTON_HEIGHT = 14;
	private static final int BUTTON_GAP = 2;
	private static final int SUMMARY_LEFT = 8;
	private static final int SUMMARY_TOP = 20;
	private static final int SUMMARY_LINE = 10;
	private static final int TEXT_COLOUR = 0x404040;

	private static final String[] LABELS = {
			EchoTranslations.UPGRADE_MEMORY_SLOT,
			EchoTranslations.UPGRADE_RADIUS,
			EchoTranslations.UPGRADE_SENSITIVITY,
			EchoTranslations.UPGRADE_CLARITY,
	};

	private final Button[] buttons = new Button[LABELS.length];

	public ResonanceTableScreen(ResonanceTableMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = PANEL_WIDTH;
		this.imageHeight = PANEL_HEIGHT;
	}

	@Override
	protected void init() {
		super.init();
		for (int index = 0; index < LABELS.length; index++) {
			int buttonIndex = index;
			buttons[index] = Button.builder(Component.translatable(LABELS[index]), button -> press(buttonIndex))
					.bounds(leftPos + BUTTON_LEFT, topPos + BUTTON_TOP + index * (BUTTON_HEIGHT + BUTTON_GAP),
							BUTTON_WIDTH, BUTTON_HEIGHT)
					.build();
			addRenderableWidget(buttons[index]);
		}
	}

	private void press(int index) {
		if (minecraft == null || minecraft.gameMode == null) {
			return;
		}
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		for (int index = 0; index < buttons.length; index++) {
			buttons[index].active = minecraft != null && minecraft.player != null
					&& menu.canAfford(minecraft.player, ResonanceTableUpgrade.fromIndex(index));
		}
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT_COLOUR, false);
		if (menu.lens().isEmpty()) {
			graphics.drawString(font, Component.translatable(EchoTranslations.UPGRADE_NO_LENS),
					SUMMARY_LEFT, SUMMARY_TOP, TEXT_COLOUR, false);
			return;
		}
		EchoLensUpgrades upgrades = menu.upgrades();
		graphics.drawString(font, Component.translatable(EchoTranslations.TOOLTIP_TUNING,
						upgrades.radius(), upgrades.sensitivity(), upgrades.clarity()),
				SUMMARY_LEFT, SUMMARY_TOP, TEXT_COLOUR, false);
		graphics.drawString(font, Component.translatable(EchoTranslations.TOOLTIP_MEMORIES,
						0, upgrades.memorySlots()),
				SUMMARY_LEFT, SUMMARY_TOP + SUMMARY_LINE, TEXT_COLOUR, false);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
		renderUpgradeCost(graphics, mouseX, mouseY);
	}

	private void renderUpgradeCost(GuiGraphics graphics, int mouseX, int mouseY) {
		for (int index = 0; index < buttons.length; index++) {
			if (!buttons[index].isMouseOver(mouseX, mouseY)) {
				continue;
			}
			ResonanceTableUpgrade upgrade = ResonanceTableUpgrade.fromIndex(index);
			if (upgrade == null) {
				return;
			}
			Component text = upgrade.amethystShards() > 0
					? Component.translatable(EchoTranslations.UPGRADE_COST_WITH_SHARDS,
							upgrade.experienceLevels(), upgrade.amethystShards())
					: Component.translatable(EchoTranslations.UPGRADE_COST, upgrade.experienceLevels());
			graphics.renderTooltip(font, text, mouseX, mouseY);
			return;
		}
	}
}
