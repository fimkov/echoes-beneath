package dev.fimkov.echoesbeneath.menu;

import dev.fimkov.echoesbeneath.memory.EchoFormat;

public final class SoundingTableLayout {
	public static final int PANEL_WIDTH = 216;
	public static final int PANEL_HEIGHT = 222;
	public static final int SLOT_SIZE = 16;

	public static final int COLUMN_X = 8;
	public static final int COLUMN_WIDTH = 62;

	public static final int LENS_SLOT_X = 31;
	public static final int LENS_SLOT_Y = 21;
	public static final int RESULT_SLOT_X = 31;
	public static final int RESULT_SLOT_Y = 101;

	public static final int ROW_COUNT = EchoFormat.MAX_MEMORY_SLOTS;
	public static final int LIST_X = COLUMN_X;
	public static final int LIST_Y = 47;
	public static final int LIST_WIDTH = COLUMN_WIDTH;
	public static final int ROW_HEIGHT = 14;
	public static final int ROW_GAP = 2;

	public static final int STAGE_X = 76;
	public static final int STAGE_Y = 18;
	public static final int STAGE_WIDTH = 114;
	public static final int STAGE_HEIGHT = 86;

	public static final int RULER_X = 196;
	public static final int RULER_Y = 18;
	public static final int RULER_WIDTH = 12;
	public static final int RULER_HEIGHT = 86;
	public static final int HANDLE_WIDTH = 16;
	public static final int HANDLE_HEIGHT = 3;

	public static final int EXTRACT_X = 76;
	public static final int EXTRACT_Y = 108;
	public static final int EXTRACT_WIDTH = 114;
	public static final int EXTRACT_HEIGHT = 16;

	public static final int TITLE_X = 8;
	public static final int TITLE_Y = 6;
	public static final int INVENTORY_LABEL_X = 27;
	public static final int INVENTORY_LABEL_Y = 129;
	public static final int INVENTORY_X = 27;
	public static final int INVENTORY_Y = 140;
	public static final int HOTBAR_Y = 198;

	private SoundingTableLayout() {
	}

	public static int rowY(int index) {
		return LIST_Y + index * (ROW_HEIGHT + ROW_GAP);
	}
}
