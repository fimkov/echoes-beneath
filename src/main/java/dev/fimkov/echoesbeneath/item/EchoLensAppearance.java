package dev.fimkov.echoesbeneath.item;

import dev.fimkov.echoesbeneath.memory.EchoFormat;

public final class EchoLensAppearance {
	public static final int RADIUS_TIERS = 3;
	public static final int HEARING_TIERS = 3;
	public static final int MEMORY_STATES = countMemoryStates();
	public static final int STATE_COUNT =
			(EchoLensUpgrades.MAX_VISION + 1) * MEMORY_STATES * RADIUS_TIERS * HEARING_TIERS;

	private EchoLensAppearance() {
	}

	private static int countMemoryStates() {
		int total = 0;
		for (int capacity = EchoFormat.DEFAULT_MEMORY_SLOTS; capacity <= EchoFormat.MAX_MEMORY_SLOTS; capacity++) {
			total += capacity + 1;
		}
		return total;
	}

	public static int radiusTier(int radius) {
		int steps = (radius - EchoLensUpgrades.MIN_RADIUS) / EchoLensUpgrades.RADIUS_STEP;
		if (steps <= 0) {
			return 0;
		}
		return steps <= 3 ? 1 : 2;
	}

	public static int hearingTier(int sensitivity) {
		if (sensitivity <= 0) {
			return 0;
		}
		return sensitivity <= 2 ? 1 : 2;
	}

	public static int memoryIndex(int capacity, int filled) {
		int safeCapacity = Math.clamp(capacity, EchoFormat.DEFAULT_MEMORY_SLOTS, EchoFormat.MAX_MEMORY_SLOTS);
		int offset = 0;
		for (int smaller = EchoFormat.DEFAULT_MEMORY_SLOTS; smaller < safeCapacity; smaller++) {
			offset += smaller + 1;
		}
		return offset + Math.clamp(filled, 0, safeCapacity);
	}

	public static int stateIndex(int vision, int capacity, int filled, int radius, int sensitivity) {
		int safeVision = Math.clamp(vision, 0, EchoLensUpgrades.MAX_VISION);
		int index = safeVision * MEMORY_STATES + memoryIndex(capacity, filled);
		index = index * RADIUS_TIERS + radiusTier(radius);
		return index * HEARING_TIERS + hearingTier(sensitivity);
	}

	public static float tuningValue(int vision, int capacity, int filled, int radius, int sensitivity) {
		return (float) stateIndex(vision, capacity, filled, radius, sensitivity) / (STATE_COUNT - 1);
	}
}
