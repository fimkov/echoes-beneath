package dev.fimkov.echoesbeneath.menu;

public final class SoundingTablePlan {
	public static final int SELECT_BASE = 0;
	public static final int LOWEST_BASE = 1000;
	public static final int HIGHEST_BASE = 3000;
	public static final int EXTRACT = 9000;
	private static final int HEIGHT_RANGE = 1024;

	private SoundingTablePlan() {
	}

	public static int selectButton(int slot) {
		return SELECT_BASE + slot;
	}

	public static int lowestButton(int worldY, int worldBottom) {
		return LOWEST_BASE + clampOffset(worldY - worldBottom);
	}

	public static int highestButton(int worldY, int worldBottom) {
		return HIGHEST_BASE + clampOffset(worldY - worldBottom);
	}

	public static int offsetOf(int button, int base) {
		return button - base;
	}

	public static boolean isSelect(int button, int slots) {
		return button >= SELECT_BASE && button < SELECT_BASE + slots;
	}

	public static boolean isLowest(int button) {
		return button >= LOWEST_BASE && button < LOWEST_BASE + HEIGHT_RANGE;
	}

	public static boolean isHighest(int button) {
		return button >= HIGHEST_BASE && button < HIGHEST_BASE + HEIGHT_RANGE;
	}

	private static int clampOffset(int offset) {
		return Math.clamp(offset, 0, HEIGHT_RANGE - 1);
	}
}
