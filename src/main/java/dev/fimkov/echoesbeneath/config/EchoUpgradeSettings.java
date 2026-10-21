package dev.fimkov.echoesbeneath.config;

import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.menu.ResonanceTableUpgrade;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public record EchoUpgradeSettings(List<Track> tracks) {
	public record Track(int maxSteps, int experiencePerStep, int shardsPerStep) {
	}

	public EchoUpgradeSettings {
		tracks = List.copyOf(tracks);
	}

	public static EchoUpgradeSettings defaults() {
		return read(new Properties());
	}

	public static EchoUpgradeSettings read(Properties properties) {
		return new EchoUpgradeSettings(Arrays.stream(ResonanceTableUpgrade.all())
				.map(upgrade -> {
					String prefix = prefix(upgrade);
					return new Track(
							integer(properties, prefix + "maxSteps", upgrade.maxSteps(), upgrade.maxSteps()),
							integer(properties, prefix + "experiencePerStep", upgrade.experiencePerStep(), 1000),
							integer(properties, prefix + "shardsPerStep", upgrade.shardsPerStep(), 64));
				}).toList());
	}

	public void write(Properties properties) {
		for (ResonanceTableUpgrade upgrade : ResonanceTableUpgrade.all()) {
			Track track = track(upgrade);
			String prefix = prefix(upgrade);
			properties.setProperty(prefix + "maxSteps", Integer.toString(track.maxSteps()));
			properties.setProperty(prefix + "experiencePerStep", Integer.toString(track.experiencePerStep()));
			properties.setProperty(prefix + "shardsPerStep", Integer.toString(track.shardsPerStep()));
		}
	}

	public Track track(ResonanceTableUpgrade upgrade) {
		return tracks.get(upgrade.ordinal());
	}

	public EchoLensUpgrades limit(EchoLensUpgrades upgrades) {
		EchoLensUpgrades result = upgrades;
		for (ResonanceTableUpgrade upgrade : ResonanceTableUpgrade.all()) {
			result = upgrade.withSteps(result, Math.min(upgrade.stepsOf(result), track(upgrade).maxSteps()));
		}
		return result;
	}

	private static String prefix(ResonanceTableUpgrade upgrade) {
		return "upgrades." + upgrade.name().toLowerCase(Locale.ROOT) + ".";
	}

	private static int integer(Properties properties, String key, int fallback, int maximum) {
		try {
			return Math.clamp(Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim()),
					0, maximum);
		} catch (NumberFormatException invalid) {
			return fallback;
		}
	}
}
