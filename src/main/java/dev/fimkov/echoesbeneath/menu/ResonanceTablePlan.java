package dev.fimkov.echoesbeneath.menu;

import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;

public record ResonanceTablePlan(EchoLensUpgrades target, int experienceLevels, int amethystShards,
		boolean changed) {
	public static final ResonanceTablePlan NONE =
			new ResonanceTablePlan(EchoLensUpgrades.DEFAULT, 0, 0, false);

	public static ResonanceTablePlan of(EchoLensUpgrades current, EchoLensUpgrades floor,
			EchoLensUpgrades peak, EchoLensUpgrades desired) {
		EchoLensUpgrades clamped = ResonanceTableUpgrade.clampToFloor(floor, desired);
		int levels = 0;
		int shards = 0;

		for (ResonanceTableUpgrade upgrade : ResonanceTableUpgrade.all()) {
			int unpaid = upgrade.stepsOf(clamped) - upgrade.stepsOf(peak);

			if (unpaid > 0) {
				levels += unpaid * upgrade.experiencePerStep();
				shards += unpaid * upgrade.shardsPerStep();
			}
		}

		return new ResonanceTablePlan(clamped, levels, shards, !clamped.equals(current));
	}

	public boolean isFree() {
		return experienceLevels == 0 && amethystShards == 0;
	}
}
