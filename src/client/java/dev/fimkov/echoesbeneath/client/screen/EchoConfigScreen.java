package dev.fimkov.echoesbeneath.client.screen;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.client.config.EchoClientConfig;
import dev.fimkov.echoesbeneath.client.config.EchoClientConfigValues;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class EchoConfigScreen extends Screen {
	private static final int WIDGET_WIDTH = 150;
	private static final int WIDGET_HEIGHT = 20;
	private static final int COLUMN_GAP = 8;
	private static final int ROW_GAP = 4;
	private static final int TOP = 42;
	private static final int TITLE_Y = 18;
	private static final int DONE_WIDTH = 200;
	private static final int BOTTOM_MARGIN = 30;

	private final Screen parent;
	private final List<Setting> settings = new ArrayList<>();
	private int waveTexelBudget;
	private int waveDetail;
	private int hologramBlockBudget;
	private int hologramDistance;
	private boolean showBlockHolograms;
	private boolean hideSkyDuringPlayback;
	private int transitionTicks;
	private int regionAmbienceVolume;
	private boolean showChartAura;
	private int chartDistance;
	private int chartBlockBudget;
	private boolean tableAnimations;
	private int darknessStrength;
	private int waveVolume;

	private record Setting(String key, int min, int max) {
	}

	public EchoConfigScreen(Screen parent) {
		super(Component.translatable(EchoTranslations.CONFIG_TITLE));
		this.parent = parent;
		EchoClientConfigValues current = EchoClientConfig.values();
		waveTexelBudget = current.waveTexelBudget();
		waveDetail = current.waveDetail();
		hologramBlockBudget = current.hologramBlockBudget();
		hologramDistance = current.hologramDistance();
		showBlockHolograms = current.showBlockHolograms();
		hideSkyDuringPlayback = current.hideSkyDuringPlayback();
		transitionTicks = current.transitionTicks();
		regionAmbienceVolume = current.regionAmbienceVolume();
		showChartAura = current.showChartAura();
		chartDistance = current.chartDistance();
		chartBlockBudget = current.chartBlockBudget();
		tableAnimations = current.tableAnimations();
		darknessStrength = current.darknessStrength();
		waveVolume = current.waveVolume();
	}

	@Override
	protected void init() {
		int left = width / 2 - WIDGET_WIDTH - COLUMN_GAP / 2;
		int right = width / 2 + COLUMN_GAP / 2;
		int row = 0;
		addSlider(left, row, "wave_budget", 2000, 160000, waveTexelBudget, value -> waveTexelBudget = value);
		addSlider(right, row++, "wave_detail", 2, 16, waveDetail, value -> waveDetail = value);
		addSlider(left, row, "hologram_budget", 0, 4096, hologramBlockBudget,
				value -> hologramBlockBudget = value);
		addSlider(right, row++, "hologram_distance", 8, 96, hologramDistance,
				value -> hologramDistance = value);
		addSlider(left, row, "transition", 1, 200, transitionTicks, value -> transitionTicks = value);
		addSlider(right, row++, "ambience", 0, 100, regionAmbienceVolume,
				value -> regionAmbienceVolume = value);
		addSlider(left, row, "darkness", 0, 100, darknessStrength, value -> darknessStrength = value);
		addSlider(right, row++, "wave_volume", 0, 100, waveVolume, value -> waveVolume = value);
		addSlider(left, row, "chart_budget", 256, 20000, chartBlockBudget,
				value -> chartBlockBudget = value);
		addSlider(right, row++, "chart_distance", 16, 128, chartDistance,
				value -> chartDistance = value);
		addToggle(left, row, "show_holograms", showBlockHolograms, value -> showBlockHolograms = value);
		addToggle(right, row++, "hide_sky", hideSkyDuringPlayback, value -> hideSkyDuringPlayback = value);
		addToggle(left, row, "chart_aura", showChartAura, value -> showChartAura = value);
		addToggle(right, row++, "table_animations", tableAnimations, value -> tableAnimations = value);

		addRenderableWidget(Button.builder(CommonLabels.done(), button -> saveAndClose())
				.bounds(width / 2 - DONE_WIDTH / 2, height - BOTTOM_MARGIN, DONE_WIDTH, WIDGET_HEIGHT)
				.build());
	}

	private int rowY(int row) {
		return TOP + row * (WIDGET_HEIGHT + ROW_GAP);
	}

	private void addSlider(int x, int row, String key, int min, int max, int initial, IntConsumer sink) {
		settings.add(new Setting(key, min, max));
		addRenderableWidget(new IntSlider(x, rowY(row), key, min, max, initial, sink));
	}

	private void addToggle(int x, int row, String key, boolean initial,
			Consumer<Boolean> sink) {
		addRenderableWidget(CycleButton.onOffBuilder(initial)
				.create(x, rowY(row), WIDGET_WIDTH, WIDGET_HEIGHT,
						Component.translatable(optionKey(key)), (button, value) -> sink.accept(value)));
	}

	private static String optionKey(String key) {
		return "config.echoes_beneath." + key;
	}

	private void saveAndClose() {
		EchoClientConfig.apply(new EchoClientConfigValues(waveTexelBudget, hologramBlockBudget,
				hologramDistance, showBlockHolograms, hideSkyDuringPlayback, transitionTicks,
				regionAmbienceVolume, waveDetail, showChartAura, chartDistance, chartBlockBudget,
				tableAnimations, darknessStrength, waveVolume));
		onClose();
	}

	@Override
	public void onClose() {
		if (minecraft != null) {
			minecraft.setScreen(parent);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, title, width / 2, TITLE_Y, 0xFFFFFF);
	}

	private static final class CommonLabels {
		private CommonLabels() {
		}

		static Component done() {
			return Component.translatable("gui.done");
		}
	}

	private static final class IntSlider extends AbstractSliderButton {
		private final String key;
		private final int min;
		private final int max;
		private final IntConsumer sink;

		IntSlider(int x, int y, String key, int min, int max, int initial, IntConsumer sink) {
			super(x, y, WIDGET_WIDTH, WIDGET_HEIGHT, Component.empty(),
					(double) (initial - min) / (max - min));
			this.key = key;
			this.min = min;
			this.max = max;
			this.sink = sink;
			updateMessage();
		}

		private int current() {
			return Mth.clamp((int) Math.round(min + value * (max - min)), min, max);
		}

		@Override

		protected void updateMessage() {
			setMessage(Component.translatable(optionKey(key)).append(": ")
					.append(Component.literal(Integer.toString(current()))));
		}

		@Override

		protected void applyValue() {
			sink.accept(current());
		}
	}
}
