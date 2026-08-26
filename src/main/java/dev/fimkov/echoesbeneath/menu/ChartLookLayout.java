package dev.fimkov.echoesbeneath.menu;

public final class ChartLookLayout {
	public static final int PANEL_WIDTH = 176;
	public static final int PANEL_HEIGHT = 128;

	public static final int ROW_COUNT = 5;
	public static final int ROW_X = 8;
	public static final int ROW_Y = 22;
	public static final int ROW_WIDTH = 160;
	public static final int ROW_HEIGHT = 18;
	public static final int ROW_GAP = 2;

	public static final int VALUE_WIDTH = 54;
	public static final int VALUE_INSET = 4;
	public static final int LABEL_INSET = 6;

	public static final int TITLE_X = 8;
	public static final int TITLE_Y = 7;

	private ChartLookLayout() {
	}

	public static int rowY(int index) {
		return ROW_Y + index * (ROW_HEIGHT + ROW_GAP);
	}

	public static int valueX() {
		return ROW_X + ROW_WIDTH - VALUE_INSET - VALUE_WIDTH;
	}
}
