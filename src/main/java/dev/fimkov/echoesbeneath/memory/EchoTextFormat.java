package dev.fimkov.echoesbeneath.memory;

import java.util.Locale;

public final class EchoTextFormat {
	private static final String[] ROMAN_NUMERALS = {"I", "II", "III"};
	private static final int TICKS_PER_SECOND = 20;
	private static final int SECONDS_PER_MINUTE = 60;

	private EchoTextFormat() {
	}

	public static String romanSlot(int slot) {
		if (slot < 0 || slot >= ROMAN_NUMERALS.length) {
			return Integer.toString(slot + 1);
		}

		return ROMAN_NUMERALS[slot];
	}

	public static String clock(int ticks) {
		int totalSeconds = Math.max(0, ticks) / TICKS_PER_SECOND;

		return String.format(Locale.ROOT, "%02d:%02d", totalSeconds / SECONDS_PER_MINUTE, totalSeconds % SECONDS_PER_MINUTE);
	}
}
