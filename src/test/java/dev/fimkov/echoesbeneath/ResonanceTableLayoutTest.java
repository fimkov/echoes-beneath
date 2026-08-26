package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.menu.ResonanceTableLayout;
import dev.fimkov.echoesbeneath.menu.ResonanceTableUpgrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResonanceTableLayoutTest {
	private record Box(String name, int x, int y, int width, int height) {
		boolean overlaps(Box other) {
			return x < other.x + other.width && other.x < x + width
					&& y < other.y + other.height && other.y < y + height;
		}

		boolean within(int width_, int height_) {
			return x >= 0 && y >= 0 && x + width <= width_ && y + height <= height_;
		}
	}

	private static List<Box> rowBoxes(int index) {
		int y = ResonanceTableLayout.rowY(index);
		int pips = ResonanceTableUpgrade.fromIndex(index).maxSteps();
		List<Box> boxes = new ArrayList<>();
		boxes.add(new Box("icon " + index, ResonanceTableLayout.ICON_X, y + ResonanceTableLayout.ICON_Y_OFFSET,
				ResonanceTableLayout.ICON_SIZE, ResonanceTableLayout.ICON_SIZE));
		boxes.add(new Box("label " + index, ResonanceTableLayout.LABEL_X, y + ResonanceTableLayout.LABEL_Y_OFFSET,
				ResonanceTableLayout.labelWidthBudget(pips), ResonanceTableLayout.TEXT_HEIGHT));
		boxes.add(new Box("pips " + index, ResonanceTableLayout.pipsLeft(pips), y + ResonanceTableLayout.PIP_Y_OFFSET,
				ResonanceTableLayout.pipsWidth(pips), ResonanceTableLayout.PIP_HEIGHT));
		boxes.add(new Box("minus " + index, ResonanceTableLayout.MINUS_X, y + ResonanceTableLayout.BUTTON_Y_OFFSET,
				ResonanceTableLayout.BUTTON_SIZE, ResonanceTableLayout.BUTTON_SIZE));
		boxes.add(new Box("plus " + index, ResonanceTableLayout.PLUS_X, y + ResonanceTableLayout.BUTTON_Y_OFFSET,
				ResonanceTableLayout.BUTTON_SIZE, ResonanceTableLayout.BUTTON_SIZE));

		return boxes;
	}

	@Test
	@DisplayName("nothing inside an upgrade row overlaps anything else in that row")
	void rowContentsDoNotCollide() {
		for (int index = 0; index < ResonanceTableLayout.ROW_COUNT; index++) {
			List<Box> boxes = rowBoxes(index);

			for (int a = 0; a < boxes.size(); a++) {
				for (int b = a + 1; b < boxes.size(); b++) {
					assertFalse(boxes.get(a).overlaps(boxes.get(b)),
							boxes.get(a).name() + " overlaps " + boxes.get(b).name());
				}
			}
		}
	}

	@Test
	@DisplayName("every row element sits inside its own row")
	void rowContentsStayInsideTheRow() {
		for (int index = 0; index < ResonanceTableLayout.ROW_COUNT; index++) {
			int top = ResonanceTableLayout.rowY(index);

			for (Box box : rowBoxes(index)) {
				assertTrue(box.x() >= ResonanceTableLayout.ROW_X, box.name() + " starts left of the row");
				assertTrue(box.x() + box.width() <= ResonanceTableLayout.ROW_X + ResonanceTableLayout.ROW_WIDTH,
						box.name() + " runs past the row");
				assertTrue(box.y() >= top, box.name() + " sits above the row");
				assertTrue(box.y() + box.height() <= top + ResonanceTableLayout.ROW_HEIGHT,
						box.name() + " sits below the row");
			}
		}
	}

	@Test
	@DisplayName("rows, slots and the inventory never overlap each other")
	void panelRegionsAreDisjoint() {
		List<Box> boxes = new ArrayList<>();

		for (int index = 0; index < ResonanceTableLayout.ROW_COUNT; index++) {
			boxes.add(new Box("row " + index, ResonanceTableLayout.ROW_X, ResonanceTableLayout.rowY(index),
					ResonanceTableLayout.ROW_WIDTH, ResonanceTableLayout.ROW_HEIGHT));
		}

		boxes.add(new Box("lens slot", ResonanceTableLayout.LENS_SLOT_X, ResonanceTableLayout.LENS_SLOT_Y,
				ResonanceTableLayout.SLOT_SIZE, ResonanceTableLayout.SLOT_SIZE));
		boxes.add(new Box("amethyst slot", ResonanceTableLayout.AMETHYST_SLOT_X, ResonanceTableLayout.AMETHYST_SLOT_Y,
				ResonanceTableLayout.SLOT_SIZE, ResonanceTableLayout.SLOT_SIZE));
		boxes.add(new Box("result slot", ResonanceTableLayout.RESULT_SLOT_X, ResonanceTableLayout.RESULT_SLOT_Y,
				ResonanceTableLayout.SLOT_SIZE, ResonanceTableLayout.SLOT_SIZE));
		boxes.add(new Box("inventory", ResonanceTableLayout.INVENTORY_LEFT, ResonanceTableLayout.INVENTORY_TOP,
				ResonanceTableLayout.INVENTORY_COLUMNS * ResonanceTableLayout.SLOT_SPACING,
				ResonanceTableLayout.INVENTORY_ROWS * ResonanceTableLayout.SLOT_SPACING));
		boxes.add(new Box("hotbar", ResonanceTableLayout.INVENTORY_LEFT, ResonanceTableLayout.HOTBAR_TOP,
				ResonanceTableLayout.INVENTORY_COLUMNS * ResonanceTableLayout.SLOT_SPACING,
				ResonanceTableLayout.SLOT_SIZE));

		for (int a = 0; a < boxes.size(); a++) {
			for (int b = a + 1; b < boxes.size(); b++) {
				assertFalse(boxes.get(a).overlaps(boxes.get(b)),
						boxes.get(a).name() + " overlaps " + boxes.get(b).name());
			}
			assertTrue(boxes.get(a).within(ResonanceTableLayout.PANEL_WIDTH, ResonanceTableLayout.PANEL_HEIGHT),
					boxes.get(a).name() + " leaves the panel");
		}
	}

	@Test
	@DisplayName("the cost readout never reaches the result dial")
	void costColumnClearsTheDial() {
		assertTrue(ResonanceTableLayout.COST_LEFT_LIMIT
				>= ResonanceTableLayout.RESULT_SLOT_X + ResonanceTableLayout.SLOT_SIZE,
				"the cost column starts inside the result slot");
		assertTrue(ResonanceTableLayout.COST_RIGHT <= ResonanceTableLayout.PANEL_WIDTH - 4,
				"the cost column runs off the panel");
	}
}
