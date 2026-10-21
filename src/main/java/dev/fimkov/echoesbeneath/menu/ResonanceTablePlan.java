package dev.fimkov.echoesbeneath.menu;

import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.config.EchoUpgradeSettings;

public record ResonanceTablePlan(EchoLensUpgrades target, int experienceLevels, int amethystShards,
		boolean changed) {
	public static final ResonanceTablePlan NONE =
			new ResonanceTablePlan(EchoLensUpgrades.DEFAULT, 0, 0, false);

	public static ResonanceTablePlan of(EchoLensUpgrades current, EchoLensUpgrades floor,
			EchoLensUpgrades peak, EchoLensUpgrades desired) {
		return of(current, floor, peak, desired, EchoUpgradeSettings.defaults());
	}

	public static ResonanceTablePlan of(EchoLensUpgrades current, EchoLensUpgrades floor,
			EchoLensUpgrades peak, EchoLensUpgrades desired, EchoUpgradeSettings settings) {
		EchoLensUpgrades clamped = ResonanceTableUpgrade.clampToFloor(floor, settings.limit(desired));
		int levels = 0;
		int shards = 0;

		for (ResonanceTableUpgrade upgrade : ResonanceTableUpgrade.all()) {
			int unpaid = upgrade.stepsOf(clamped) - upgrade.stepsOf(peak);

			if (unpaid > 0) {
				levels += unpaid * settings.track(upgrade).experiencePerStep();
				shards += unpaid * settings.track(upgrade).shardsPerStep();
			}
		}

		return new ResonanceTablePlan(clamped, levels, shards, !clamped.equals(current));
	}

	public boolean isFree() {
		return experienceLevels == 0 && amethystShards == 0;
	}
}
