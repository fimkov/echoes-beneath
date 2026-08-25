package dev.fimkov.echoesbeneath.menu;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.memory.EchoFormat;

import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;

public enum ResonanceTableUpgrade {
	MEMORY_SLOT(EchoTranslations.UPGRADE_MEMORY_SLOT, EchoTranslations.UPGRADE_UNIT_MEMORIES,
			EchoFormat.MAX_MEMORY_SLOTS - EchoFormat.DEFAULT_MEMORY_SLOTS, 12, 8,
			upgrades -> upgrades.memorySlots() - EchoFormat.DEFAULT_MEMORY_SLOTS,
			steps -> EchoFormat.DEFAULT_MEMORY_SLOTS + steps),
	RADIUS(EchoTranslations.UPGRADE_RADIUS, EchoTranslations.UPGRADE_UNIT_BLOCKS,
			(EchoLensUpgrades.MAX_RADIUS - EchoLensUpgrades.MIN_RADIUS) / EchoLensUpgrades.RADIUS_STEP, 6, 0,
			upgrades -> (upgrades.radius() - EchoLensUpgrades.MIN_RADIUS) / EchoLensUpgrades.RADIUS_STEP,
			steps -> EchoLensUpgrades.MIN_RADIUS + steps * EchoLensUpgrades.RADIUS_STEP),
	SENSITIVITY(EchoTranslations.UPGRADE_SENSITIVITY, EchoTranslations.UPGRADE_UNIT_LEVEL,
			EchoLensUpgrades.MAX_SENSITIVITY, 9, 0,
			EchoLensUpgrades::sensitivity, steps -> steps),
	CLARITY(EchoTranslations.UPGRADE_CLARITY, EchoTranslations.UPGRADE_UNIT_LEVEL,
			EchoLensUpgrades.MAX_CLARITY, 20, 0,
			EchoLensUpgrades::clarity, steps -> steps);

	private static final ResonanceTableUpgrade[] VALUES = values();

	private final String nameKey;
	private final String unitKey;
	private final int maxSteps;
	private final int experiencePerStep;
	private final int shardsPerStep;
	private final ToIntFunction<EchoLensUpgrades> reader;
	private final IntUnaryOperator display;

	ResonanceTableUpgrade(String nameKey, String unitKey, int maxSteps, int experiencePerStep, int shardsPerStep,
			ToIntFunction<EchoLensUpgrades> reader, IntUnaryOperator display) {
		this.nameKey = nameKey;
		this.unitKey = unitKey;
		this.maxSteps = maxSteps;
		this.experiencePerStep = experiencePerStep;
		this.shardsPerStep = shardsPerStep;
		this.reader = reader;
		this.display = display;
	}

	public static ResonanceTableUpgrade[] all() {
		return VALUES;
	}

	public static ResonanceTableUpgrade fromIndex(int index) {
		return index < 0 || index >= VALUES.length ? null : VALUES[index];
	}

	public String nameKey() {
		return nameKey;
	}

	public String unitKey() {
		return unitKey;
	}

	public int maxSteps() {
		return maxSteps;
	}

	public int experiencePerStep() {
		return experiencePerStep;
	}

	public int shardsPerStep() {
		return shardsPerStep;
	}

	public int stepsOf(EchoLensUpgrades upgrades) {
		return Math.clamp(reader.applyAsInt(upgrades), 0, maxSteps);
	}

	public int displayValue(int steps) {
		return display.applyAsInt(Math.clamp(steps, 0, maxSteps));
	}

	public EchoLensUpgrades withSteps(EchoLensUpgrades upgrades, int steps) {
		int clamped = Math.clamp(steps, 0, maxSteps);
		return switch (this) {
			case MEMORY_SLOT -> new EchoLensUpgrades(displayValue(clamped), upgrades.radius(),
					upgrades.sensitivity(), upgrades.clarity());
			case RADIUS -> new EchoLensUpgrades(upgrades.memorySlots(), displayValue(clamped),
					upgrades.sensitivity(), upgrades.clarity());
			case SENSITIVITY -> new EchoLensUpgrades(upgrades.memorySlots(), upgrades.radius(),
					clamped, upgrades.clarity());
			case CLARITY -> new EchoLensUpgrades(upgrades.memorySlots(), upgrades.radius(),
					upgrades.sensitivity(), clamped);
		};
	}

	public static EchoLensUpgrades clampToFloor(EchoLensUpgrades floor, EchoLensUpgrades target) {
		EchoLensUpgrades result = target;
		for (ResonanceTableUpgrade upgrade : VALUES) {
			int lowest = upgrade.stepsOf(floor);
			result = upgrade.withSteps(result, Math.max(lowest, upgrade.stepsOf(result)));
		}
		return result;
	}

	public static int buttonId(int index, boolean increase) {
		return index * 2 + (increase ? 1 : 0);
	}

	public static int indexOfButton(int buttonId) {
		return buttonId >> 1;
	}

	public static boolean isIncrease(int buttonId) {
		return (buttonId & 1) == 1;
	}
}
