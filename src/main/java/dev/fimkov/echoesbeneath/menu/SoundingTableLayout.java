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
	public static final int STAGE_WIDTH = 132;
	public static final int STAGE_HEIGHT = 24;

	public static final int START_Y = 46;
	public static final int END_Y = 64;
	public static final int VOLUME_Y = 82;
	public static final int SLIDER_HEIGHT = 18;
	public static final int PREVIEW_X = 76;
	public static final int ACTION_WIDTH = 64;
	public static final int EXTRACT_X = 144;
	public static final int EXTRACT_Y = 106;
	public static final int EXTRACT_WIDTH = 64;
	public static final int EXTRACT_HEIGHT = 18;

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
