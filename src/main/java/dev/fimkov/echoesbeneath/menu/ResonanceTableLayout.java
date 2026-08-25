package dev.fimkov.echoesbeneath.menu;

public final class ResonanceTableLayout {
	public static final int PANEL_WIDTH = 176;
	public static final int PANEL_HEIGHT = 225;

	public static final int SLOT_SIZE = 16;
	public static final int LENS_SLOT_X = 18;
	public static final int LENS_SLOT_Y = 24;
	public static final int AMETHYST_SLOT_X = 18;
	public static final int AMETHYST_SLOT_Y = 44;
	public static final int RESULT_SLOT_X = 88;
	public static final int RESULT_SLOT_Y = 34;

	public static final int TITLE_X = 10;
	public static final int TITLE_Y = 6;
	public static final int COST_X = 124;
	public static final int COST_Y = 26;
	public static final int COST_LINE = 10;
	public static final int CHAMBER_BOTTOM = 66;
	public static final int COST_RIGHT = PANEL_WIDTH - 8;
	public static final int COST_LEFT_LIMIT = 118;
	public static final int COST_SHARD_Y = 48;
	public static final int SHARD_ICON_SIZE = 16;
	public static final int DIAL_CENTRE_X = 96;
	public static final int DIAL_CENTRE_Y = 42;
	public static final int DIAL_GLOW_SIZE = 32;

	public static final int ROW_X = 8;
	public static final int ROW_WIDTH = 160;
	public static final int ROW_HEIGHT = 14;
	public static final int ROW_SPACING = 15;
	public static final int ROW_TOP = 72;
	public static final int ROW_COUNT = 4;

	public static final int ICON_X = ROW_X + 3;
	public static final int ICON_Y_OFFSET = 3;
	public static final int ICON_SIZE = 8;
	public static final int LABEL_X = 23;
	public static final int LABEL_Y_OFFSET = 3;
	public static final int TEXT_HEIGHT = 8;

	public static final int PIP_RIGHT = 132;
	public static final int PIP_WIDTH = 4;
	public static final int PIP_HEIGHT = 6;
	public static final int PIP_GAP = 1;
	public static final int PIP_Y_OFFSET = 4;

	public static final int BUTTON_SIZE = 12;
	public static final int BUTTON_Y_OFFSET = 1;
	public static final int MINUS_X = 137;
	public static final int PLUS_X = 152;

	public static final int INVENTORY_LEFT = 8;
	public static final int INVENTORY_TOP = 145;
	public static final int HOTBAR_TOP = 203;
	public static final int INVENTORY_LABEL_X = 8;
	public static final int INVENTORY_LABEL_Y = 135;
	public static final int SLOT_SPACING = 18;
	public static final int INVENTORY_ROWS = 3;
	public static final int INVENTORY_COLUMNS = 9;

	private ResonanceTableLayout() {
	}

	public static int rowY(int index) {
		return ROW_TOP + index * ROW_SPACING;
	}

	public static int pipsWidth(int count) {
		return count <= 0 ? 0 : count * PIP_WIDTH + (count - 1) * PIP_GAP;
	}

	public static int pipsLeft(int count) {
		return PIP_RIGHT - pipsWidth(count);
	}

	public static int labelWidthBudget(int pipCount) {
		return pipsLeft(pipCount) - LABEL_X - PIP_GAP * 2;
	}
}
