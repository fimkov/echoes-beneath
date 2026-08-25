package dev.fimkov.echoesbeneath.menu;

import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;

public record ResonanceTablePlan(EchoLensUpgrades floor, EchoLensUpgrades target,
		int experienceLevels, int amethystShards) {
	public static final ResonanceTablePlan NONE =
			new ResonanceTablePlan(EchoLensUpgrades.DEFAULT, EchoLensUpgrades.DEFAULT, 0, 0);

	public static ResonanceTablePlan between(EchoLensUpgrades floor, EchoLensUpgrades target) {
		EchoLensUpgrades clamped = ResonanceTableUpgrade.clampToFloor(floor, target);
		int levels = 0;
		int shards = 0;
		for (ResonanceTableUpgrade upgrade : ResonanceTableUpgrade.all()) {
			int steps = upgrade.stepsOf(clamped) - upgrade.stepsOf(floor);
			levels += steps * upgrade.experiencePerStep();
			shards += steps * upgrade.shardsPerStep();
		}
		return new ResonanceTablePlan(floor, clamped, levels, shards);
	}

	public boolean isEmpty() {
		return target.equals(floor);
	}

	public int stepsAdded(ResonanceTableUpgrade upgrade) {
		return upgrade.stepsOf(target) - upgrade.stepsOf(floor);
	}
}
