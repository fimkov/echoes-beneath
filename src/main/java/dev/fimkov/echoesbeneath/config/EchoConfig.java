package dev.fimkov.echoesbeneath.config;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public final class EchoConfig {
	public static final String FILE_NAME = "echoes_beneath.properties";

	private static final int DEFAULT_RECORDING_RADIUS = 50;
	private static final int DEFAULT_RECORDING_VERTICAL_RADIUS = 32;
	private static final int DEFAULT_MAX_RECORDING_SECONDS = 120;
	private static final int DEFAULT_ENTITY_SAMPLE_RATE = 5;
	private static final int DEFAULT_MAX_RECORDED_ENTITIES = 128;
	private static final int DEFAULT_MAX_SOUND_EVENTS = 4096;
	private static final int DEFAULT_MAX_PARTICLE_EVENTS = 8192;
	private static final int DEFAULT_MAX_CONCURRENT_SOUND_WAVES = 64;
	private static final boolean DEFAULT_ALLOW_OVERLAPPING_PLAYBACK = false;
	private static final boolean DEFAULT_SUPPRESS_HOSTILE_MOBS = true;
	private static final boolean DEFAULT_ALLOW_CHARTS = true;
	private static final int DEFAULT_CHART_BLOCK_LIMIT = 60000;
	private static final int DEFAULT_SOUND_CARRY_BLOCKS = 16;
	private static final int MIN_CHART_BLOCK_LIMIT = 1000;
	private static final int MAX_CHART_BLOCK_LIMIT = 200000;
	private static final int MIN_SOUND_CARRY = 4;
	private static final int MAX_SOUND_CARRY = 64;
	private static final boolean DEFAULT_DISABLE_BLOCK_INTERACTION_DURING_PLAYBACK = true;

	private static final int MIN_RADIUS = 8;
	private static final int MAX_RADIUS = 64;
	private static final int MIN_VERTICAL_RADIUS = 4;
	private static final int MAX_VERTICAL_RADIUS = 192;
	private static final int MIN_RECORDING_SECONDS = 5;
	private static final int MAX_RECORDING_SECONDS = 120;
	private static final int MIN_SAMPLE_RATE = 1;
	private static final int MAX_SAMPLE_RATE = 20;
	private static final int MIN_RECORDED_ENTITIES = 1;
	private static final int MAX_RECORDED_ENTITIES = 512;
	private static final int MIN_SOUND_EVENTS = 64;
	private static final int MAX_SOUND_EVENTS = 32768;
	private static final int MIN_PARTICLE_EVENTS = 0;
	private static final int MAX_PARTICLE_EVENTS = 65536;
	private static final int MIN_CONCURRENT_WAVES = 4;
	private static final int MAX_CONCURRENT_WAVES = 256;

	private static volatile EchoConfigValues values = defaults();

	private EchoConfig() {
	}

	public static EchoConfigValues values() {
		return values;
	}

	public static EchoConfigValues defaults() {
		return new EchoConfigValues(
				DEFAULT_RECORDING_RADIUS,
				DEFAULT_RECORDING_VERTICAL_RADIUS,
				DEFAULT_MAX_RECORDING_SECONDS,
				DEFAULT_ENTITY_SAMPLE_RATE,
				DEFAULT_MAX_RECORDED_ENTITIES,
				DEFAULT_MAX_SOUND_EVENTS,
				DEFAULT_MAX_PARTICLE_EVENTS,
				DEFAULT_MAX_CONCURRENT_SOUND_WAVES,
				DEFAULT_ALLOW_OVERLAPPING_PLAYBACK,
				DEFAULT_SUPPRESS_HOSTILE_MOBS,
				DEFAULT_DISABLE_BLOCK_INTERACTION_DURING_PLAYBACK,
				DEFAULT_ALLOW_CHARTS,
				DEFAULT_CHART_BLOCK_LIMIT,
				DEFAULT_SOUND_CARRY_BLOCKS,
				false,
				EchoUpgradeSettings.defaults());
	}

	public static void load() {
		Path path = EchoPlatforms.configDirectory().resolve(FILE_NAME);
		Properties properties = new Properties();

		if (Files.isRegularFile(path)) {
			try (InputStream in = Files.newInputStream(path)) {
				properties.load(in);
			} catch (IOException failure) {
				EchoesBeneath.LOGGER.warn("Falling back to default configuration, unable to read {}", path, failure);
			}
		}

		values = sanitize(properties);
		write(path);
	}

	public static EchoConfigValues sanitize(Properties properties) {
		return new EchoConfigValues(
				clampInt(properties, "recordingRadius", DEFAULT_RECORDING_RADIUS, MIN_RADIUS, MAX_RADIUS),
				clampInt(properties, "recordingVerticalRadius", DEFAULT_RECORDING_VERTICAL_RADIUS, MIN_VERTICAL_RADIUS, MAX_VERTICAL_RADIUS),
				clampInt(properties, "maxRecordingSeconds", DEFAULT_MAX_RECORDING_SECONDS, MIN_RECORDING_SECONDS, MAX_RECORDING_SECONDS),
				clampInt(properties, "entitySampleRate", DEFAULT_ENTITY_SAMPLE_RATE, MIN_SAMPLE_RATE, MAX_SAMPLE_RATE),
				clampInt(properties, "maxRecordedEntities", DEFAULT_MAX_RECORDED_ENTITIES, MIN_RECORDED_ENTITIES, MAX_RECORDED_ENTITIES),
				clampInt(properties, "maxSoundEvents", DEFAULT_MAX_SOUND_EVENTS, MIN_SOUND_EVENTS, MAX_SOUND_EVENTS),
				clampInt(properties, "maxParticleEvents", DEFAULT_MAX_PARTICLE_EVENTS, MIN_PARTICLE_EVENTS, MAX_PARTICLE_EVENTS),
				clampInt(properties, "maxConcurrentSoundWaves", DEFAULT_MAX_CONCURRENT_SOUND_WAVES, MIN_CONCURRENT_WAVES, MAX_CONCURRENT_WAVES),
				readBoolean(properties, "allowOverlappingPlayback", DEFAULT_ALLOW_OVERLAPPING_PLAYBACK),
				readBoolean(properties, "suppressHostileMobs", DEFAULT_SUPPRESS_HOSTILE_MOBS),
				readBoolean(properties, "disableBlockInteractionDuringPlayback", DEFAULT_DISABLE_BLOCK_INTERACTION_DURING_PLAYBACK),
				readBoolean(properties, "allowCharts", DEFAULT_ALLOW_CHARTS),
				clampInt(properties, "chartBlockLimit", DEFAULT_CHART_BLOCK_LIMIT,
						MIN_CHART_BLOCK_LIMIT, MAX_CHART_BLOCK_LIMIT),
				clampInt(properties, "soundCarryBlocks", DEFAULT_SOUND_CARRY_BLOCKS,
						MIN_SOUND_CARRY, MAX_SOUND_CARRY),
				readBoolean(properties, "sphericalCharts", false),
				EchoUpgradeSettings.read(properties));
	}

	private static void write(Path path) {
		Properties out = new Properties();
		EchoConfigValues current = values;
		out.setProperty("recordingRadius", Integer.toString(current.recordingRadius()));
		out.setProperty("recordingVerticalRadius", Integer.toString(current.recordingVerticalRadius()));
		out.setProperty("maxRecordingSeconds", Integer.toString(current.maxRecordingSeconds()));
		out.setProperty("entitySampleRate", Integer.toString(current.entitySampleRate()));
		out.setProperty("maxRecordedEntities", Integer.toString(current.maxRecordedEntities()));
		out.setProperty("maxSoundEvents", Integer.toString(current.maxSoundEvents()));
		out.setProperty("maxParticleEvents", Integer.toString(current.maxParticleEvents()));
		out.setProperty("maxConcurrentSoundWaves", Integer.toString(current.maxConcurrentSoundWaves()));
		out.setProperty("allowOverlappingPlayback", Boolean.toString(current.allowOverlappingPlayback()));
		out.setProperty("suppressHostileMobs", Boolean.toString(current.suppressHostileMobs()));
		out.setProperty("disableBlockInteractionDuringPlayback", Boolean.toString(current.disableBlockInteractionDuringPlayback()));
		out.setProperty("allowCharts", Boolean.toString(current.allowCharts()));
		out.setProperty("chartBlockLimit", Integer.toString(current.chartBlockLimit()));
		out.setProperty("soundCarryBlocks", Integer.toString(current.soundCarryBlocks()));
		out.setProperty("sphericalCharts", Boolean.toString(current.sphericalCharts()));
		current.upgrades().write(out);

		try {
			Files.createDirectories(path.getParent());

			try (OutputStream stream = Files.newOutputStream(path)) {
				out.store(stream, "Echoes Beneath server configuration");
			}
		} catch (IOException failure) {
			EchoesBeneath.LOGGER.warn("Unable to persist configuration to {}", path, failure);
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
			EchoesBeneath.LOGGER.warn("Configuration key {} is not an integer, using {}", key, fallback);

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

		EchoesBeneath.LOGGER.warn("Configuration key {} is not a boolean, using {}", key, fallback);

		return fallback;
	}
}
