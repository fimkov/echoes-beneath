package dev.fimkov.echoesbeneath.client.config;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.platform.EchoPlatforms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public final class EchoClientConfig {
	public static final String FILE_NAME = "echoes_beneath_client.properties";

	private static final int DEFAULT_WAVE_TEXEL_BUDGET = 42000;
	private static final int DEFAULT_HOLOGRAM_BLOCK_BUDGET = 384;
	private static final int DEFAULT_HOLOGRAM_DISTANCE = 32;
	private static final boolean DEFAULT_SHOW_BLOCK_HOLOGRAMS = true;
	private static final boolean DEFAULT_HIDE_SKY = true;
	private static final int DEFAULT_TRANSITION_TICKS = 22;
	private static final int DEFAULT_AMBIENCE_VOLUME = 55;
	private static final int DEFAULT_WAVE_DETAIL = 8;
	private static final boolean DEFAULT_SHOW_CHART_AURA = true;
	private static final int DEFAULT_CHART_DISTANCE = 48;
	private static final int DEFAULT_CHART_BLOCK_BUDGET = 6000;
	private static final boolean DEFAULT_TABLE_ANIMATIONS = true;
	private static final int DEFAULT_DARKNESS_STRENGTH = 100;
	private static final int DEFAULT_WAVE_VOLUME = 80;

	private static final int MIN_WAVE_TEXEL_BUDGET = 2000;
	private static final int MAX_WAVE_TEXEL_BUDGET = 160000;
	private static final int MIN_HOLOGRAM_BLOCK_BUDGET = 0;
	private static final int MAX_HOLOGRAM_BLOCK_BUDGET = 4096;
	private static final int MIN_HOLOGRAM_DISTANCE = 8;
	private static final int MAX_HOLOGRAM_DISTANCE = 96;
	private static final int MIN_TRANSITION_TICKS = 1;
	private static final int MAX_TRANSITION_TICKS = 200;
	private static final int MIN_VOLUME = 0;
	private static final int MAX_VOLUME = 100;
	private static final int MIN_WAVE_DETAIL = 2;
	private static final int MAX_WAVE_DETAIL = 16;
	private static final int MIN_CHART_DISTANCE = 16;
	private static final int MAX_CHART_DISTANCE = 128;
	private static final int MIN_CHART_BLOCK_BUDGET = 256;
	private static final int MAX_CHART_BLOCK_BUDGET = 20000;

	private static volatile EchoClientConfigValues values = defaults();

	private EchoClientConfig() {
	}

	public static EchoClientConfigValues values() {
		return values;
	}

	public static EchoClientConfigValues defaults() {
		return new EchoClientConfigValues(
				DEFAULT_WAVE_TEXEL_BUDGET,
				DEFAULT_HOLOGRAM_BLOCK_BUDGET,
				DEFAULT_HOLOGRAM_DISTANCE,
				DEFAULT_SHOW_BLOCK_HOLOGRAMS,
				DEFAULT_HIDE_SKY,
				DEFAULT_TRANSITION_TICKS,
				DEFAULT_AMBIENCE_VOLUME,
				DEFAULT_WAVE_DETAIL,
				DEFAULT_SHOW_CHART_AURA,
				DEFAULT_CHART_DISTANCE,
				DEFAULT_CHART_BLOCK_BUDGET,
				DEFAULT_TABLE_ANIMATIONS,
				DEFAULT_DARKNESS_STRENGTH,
				DEFAULT_WAVE_VOLUME);
	}

	public static void apply(EchoClientConfigValues updated) {
		values = updated;
		write(path());
	}

	public static void load() {
		Path path = path();
		Properties properties = new Properties();

		if (Files.isRegularFile(path)) {
			try (InputStream stream = Files.newInputStream(path)) {
				properties.load(stream);
			} catch (IOException failure) {
				EchoesBeneath.LOGGER.warn("Unable to read {}, using defaults", path, failure);
			}
		}

		values = parse(properties);
		write(path);
	}

	private static Path path() {
		return EchoPlatforms.configDirectory().resolve(FILE_NAME);
	}

	private static EchoClientConfigValues parse(Properties properties) {
		return new EchoClientConfigValues(
				clampInt(properties, "waveTexelBudget", DEFAULT_WAVE_TEXEL_BUDGET,
						MIN_WAVE_TEXEL_BUDGET, MAX_WAVE_TEXEL_BUDGET),
				clampInt(properties, "hologramBlockBudget", DEFAULT_HOLOGRAM_BLOCK_BUDGET,
						MIN_HOLOGRAM_BLOCK_BUDGET, MAX_HOLOGRAM_BLOCK_BUDGET),
				clampInt(properties, "hologramDistance", DEFAULT_HOLOGRAM_DISTANCE,
						MIN_HOLOGRAM_DISTANCE, MAX_HOLOGRAM_DISTANCE),
				readBoolean(properties, "showBlockHolograms", DEFAULT_SHOW_BLOCK_HOLOGRAMS),
				readBoolean(properties, "hideSkyDuringPlayback", DEFAULT_HIDE_SKY),
				clampInt(properties, "transitionTicks", DEFAULT_TRANSITION_TICKS,
						MIN_TRANSITION_TICKS, MAX_TRANSITION_TICKS),
				clampInt(properties, "regionAmbienceVolume", DEFAULT_AMBIENCE_VOLUME, MIN_VOLUME, MAX_VOLUME),
				clampInt(properties, "waveDetail", DEFAULT_WAVE_DETAIL, MIN_WAVE_DETAIL, MAX_WAVE_DETAIL),
				readBoolean(properties, "showChartAura", DEFAULT_SHOW_CHART_AURA),
				clampInt(properties, "chartDistance", DEFAULT_CHART_DISTANCE,
						MIN_CHART_DISTANCE, MAX_CHART_DISTANCE),
				clampInt(properties, "chartBlockBudget", DEFAULT_CHART_BLOCK_BUDGET,
						MIN_CHART_BLOCK_BUDGET, MAX_CHART_BLOCK_BUDGET),
				readBoolean(properties, "tableAnimations", DEFAULT_TABLE_ANIMATIONS),
				clampInt(properties, "darknessStrength", DEFAULT_DARKNESS_STRENGTH, MIN_VOLUME, MAX_VOLUME),
				clampInt(properties, "waveVolume", DEFAULT_WAVE_VOLUME, MIN_VOLUME, MAX_VOLUME));
	}

	private static void write(Path path) {
		EchoClientConfigValues current = values;
		Properties out = new Properties();
		out.setProperty("waveTexelBudget", Integer.toString(current.waveTexelBudget()));
		out.setProperty("hologramBlockBudget", Integer.toString(current.hologramBlockBudget()));
		out.setProperty("hologramDistance", Integer.toString(current.hologramDistance()));
		out.setProperty("showBlockHolograms", Boolean.toString(current.showBlockHolograms()));
		out.setProperty("hideSkyDuringPlayback", Boolean.toString(current.hideSkyDuringPlayback()));
		out.setProperty("transitionTicks", Integer.toString(current.transitionTicks()));
		out.setProperty("regionAmbienceVolume", Integer.toString(current.regionAmbienceVolume()));
		out.setProperty("waveDetail", Integer.toString(current.waveDetail()));
		out.setProperty("showChartAura", Boolean.toString(current.showChartAura()));
		out.setProperty("chartDistance", Integer.toString(current.chartDistance()));
		out.setProperty("chartBlockBudget", Integer.toString(current.chartBlockBudget()));
		out.setProperty("tableAnimations", Boolean.toString(current.tableAnimations()));
		out.setProperty("darknessStrength", Integer.toString(current.darknessStrength()));
		out.setProperty("waveVolume", Integer.toString(current.waveVolume()));

		try {
			Files.createDirectories(path.getParent());

			try (OutputStream stream = Files.newOutputStream(path)) {
				out.store(stream, "Echoes Beneath client configuration");
			}
		} catch (IOException failure) {
			EchoesBeneath.LOGGER.warn("Unable to persist client configuration to {}", path, failure);
		}
	}

	private static int clampInt(Properties properties, String key, int fallback, int min, int max) {
		String raw = properties.getProperty(key);

		if (raw == null) {
			return fallback;
		}

		try {
			return Math.clamp(Integer.parseInt(raw.trim()), min, max);
		} catch (NumberFormatException invalid) {
			EchoesBeneath.LOGGER.warn("Client configuration key {} is not an integer, using {}", key, fallback);

			return fallback;
		}
	}

	private static boolean readBoolean(Properties properties, String key, boolean fallback) {
		String raw = properties.getProperty(key);

		if (raw == null) {
			return fallback;
		}

		String normalized = raw.trim().toLowerCase(Locale.ROOT);

		if (normalized.equals("true") || normalized.equals("false")) {
			return Boolean.parseBoolean(normalized);
		}

		EchoesBeneath.LOGGER.warn("Client configuration key {} is not a boolean, using {}", key, fallback);

		return fallback;
	}
}
