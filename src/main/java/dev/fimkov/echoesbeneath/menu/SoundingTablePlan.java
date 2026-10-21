package dev.fimkov.echoesbeneath.menu;

public final class SoundingTablePlan {
	public static final int SELECT_BASE = 0;
	public static final int START_BASE = 1000;
	public static final int END_BASE = 4000;
	public static final int VOLUME_BASE = 7000;
	public static final int PREVIEW = 8000;
	public static final int STOP = 8001;
	public static final int EXTRACT = 9000;

	private SoundingTablePlan() {
	}

	public static int selectButton(int slot) {
		return SELECT_BASE + slot;
	}

	public static boolean isSelect(int button, int slots) {
		return button >= SELECT_BASE && button < SELECT_BASE + slots;
	}
}
