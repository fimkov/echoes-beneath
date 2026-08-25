package dev.fimkov.echoesbeneath.menu;

import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;

import java.util.function.Function;
import java.util.function.Predicate;

public enum ResonanceTableUpgrade {
	MEMORY_SLOT(12, 8, EchoLensUpgrades::canAddMemorySlot, EchoLensUpgrades::withMemorySlot),
	RADIUS(6, 0, EchoLensUpgrades::canWidenRadius, EchoLensUpgrades::withRadius),
	SENSITIVITY(9, 0, EchoLensUpgrades::canSharpenSensitivity, EchoLensUpgrades::withSensitivity),
	CLARITY(20, 0, EchoLensUpgrades::canClearFurther, EchoLensUpgrades::withClarity);

	private static final ResonanceTableUpgrade[] VALUES = values();

	private final int experienceLevels;
	private final int amethystShards;
	private final Predicate<EchoLensUpgrades> available;
	private final Function<EchoLensUpgrades, EchoLensUpgrades> applier;

	ResonanceTableUpgrade(int experienceLevels, int amethystShards,
			Predicate<EchoLensUpgrades> available, Function<EchoLensUpgrades, EchoLensUpgrades> applier) {
		this.experienceLevels = experienceLevels;
		this.amethystShards = amethystShards;
		this.available = available;
		this.applier = applier;
	}

	public static ResonanceTableUpgrade fromIndex(int index) {
		if (index < 0 || index >= VALUES.length) {
			return null;
		}
		return VALUES[index];
	}

	public int experienceLevels() {
		return experienceLevels;
	}

	public int amethystShards() {
		return amethystShards;
	}

	public boolean isAvailable(EchoLensUpgrades upgrades) {
		return available.test(upgrades);
	}

	public EchoLensUpgrades apply(EchoLensUpgrades upgrades) {
		return applier.apply(upgrades);
	}
}
