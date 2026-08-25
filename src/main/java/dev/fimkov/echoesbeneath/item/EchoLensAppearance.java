package dev.fimkov.echoesbeneath.item;

import dev.fimkov.echoesbeneath.memory.EchoFormat;

public final class EchoLensAppearance {
	public static final int STATE_COUNT = statesPerVision() * (EchoLensUpgrades.MAX_VISION + 1);

	private EchoLensAppearance() {
	}

	private static int statesPerVision() {
		int total = 0;
		for (int capacity = EchoFormat.DEFAULT_MEMORY_SLOTS; capacity <= EchoFormat.MAX_MEMORY_SLOTS; capacity++) {
			total += capacity + 1;
		}
		return total;
	}

	public static int stateIndex(int vision, int capacity, int filled) {
		int safeVision = Math.clamp(vision, 0, EchoLensUpgrades.MAX_VISION);
		int safeCapacity = Math.clamp(capacity, EchoFormat.DEFAULT_MEMORY_SLOTS, EchoFormat.MAX_MEMORY_SLOTS);
		int safeFilled = Math.clamp(filled, 0, safeCapacity);
		int offset = 0;
		for (int smaller = EchoFormat.DEFAULT_MEMORY_SLOTS; smaller < safeCapacity; smaller++) {
			offset += smaller + 1;
		}
		return safeVision * statesPerVision() + offset + safeFilled;
	}

	public static float tuningValue(int vision, int capacity, int filled) {
		return (float) stateIndex(vision, capacity, filled) / (STATE_COUNT - 1);
	}
}
