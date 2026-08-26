package dev.fimkov.echoesbeneath.menu;

import dev.fimkov.echoesbeneath.memory.EchoFormat;

public final class SoundingTableLayout {
	public static final int PANEL_WIDTH = 202;
	public static final int PANEL_HEIGHT = 222;
	public static final int SLOT_SIZE = 16;

	public static final int LENS_SLOT_X = 12;
	public static final int LENS_SLOT_Y = 22;
	public static final int RESULT_SLOT_X = 12;
	public static final int RESULT_SLOT_Y = 100;

	public static final int ROW_COUNT = EchoFormat.MAX_MEMORY_SLOTS;
	public static final int LIST_X = 8;
	public static final int LIST_Y = 46;
	public static final int LIST_WIDTH = 44;
	public static final int ROW_HEIGHT = 13;
	public static final int ROW_GAP = 1;

	public static final int STAGE_X = 58;
	public static final int STAGE_Y = 18;
	public static final int STAGE_WIDTH = 108;
	public static final int STAGE_HEIGHT = 86;

	public static final int RULER_X = 174;
	public static final int RULER_Y = 18;
	public static final int RULER_WIDTH = 10;
	public static final int RULER_HEIGHT = 86;
	public static final int HANDLE_WIDTH = 14;
	public static final int HANDLE_HEIGHT = 3;

	public static final int EXTRACT_X = 58;
	public static final int EXTRACT_Y = 108;
	public static final int EXTRACT_WIDTH = 108;
	public static final int EXTRACT_HEIGHT = 16;

	public static final int TITLE_X = 8;
	public static final int TITLE_Y = 6;
	public static final int INVENTORY_LABEL_X = 20;
	public static final int INVENTORY_LABEL_Y = 129;
	public static final int INVENTORY_X = 20;
	public static final int INVENTORY_Y = 140;
	public static final int HOTBAR_Y = 198;

	public static final int ROW_SPRITE_U = 0;
	public static final int ROW_SPRITE_V = 223;
	public static final int BUTTON_SPRITE_U = 46;
	public static final int BUTTON_SPRITE_V = 223;
	public static final int HANDLE_SPRITE_U = 204;
	public static final int HANDLE_SPRITE_V = 0;

	private SoundingTableLayout() {
	}

	public static int rowY(int index) {
		return LIST_Y + index * (ROW_HEIGHT + ROW_GAP);
	}
}
