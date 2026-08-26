package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.menu.ResonanceTableLayout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResonanceTableWiringTest {
	private static final int[][][] EVERY_RUN = {
			ResonanceTableLayout.FEEDER_FROM_LENS,
			ResonanceTableLayout.FEEDER_FROM_AMETHYST,
			ResonanceTableLayout.TRUNK_TO_DIAL,
	};

	@Test
	@DisplayName("every wire is an unbroken run of neighbouring pixels")
	void wiresAreContinuous() {
		for (int[][] corners : EVERY_RUN) {
			List<int[]> path = ResonanceTableLayout.trace(corners);
			assertFalse(path.isEmpty(), "a wire traced to nothing");
			for (int index = 1; index < path.size(); index++) {
				int[] previous = path.get(index - 1);
				int[] current = path.get(index);
				int distance = Math.abs(current[0] - previous[0]) + Math.abs(current[1] - previous[1]);
				assertEquals(1, distance,
						"wire jumps from " + previous[0] + "," + previous[1]
								+ " to " + current[0] + "," + current[1]);
			}
		}
	}

	@Test
	@DisplayName("a wire never doubles back over a pixel it has already lit")
	void wiresDoNotRepeat() {
		for (int[][] corners : EVERY_RUN) {
			Set<String> seen = new HashSet<>();
			for (int[] point : ResonanceTableLayout.trace(corners)) {
				assertTrue(seen.add(point[0] + "," + point[1]),
						"wire revisits " + point[0] + "," + point[1]);
			}
		}
	}

	@Test
	@DisplayName("both feeders start at their slot and meet where the trunk begins")
	void feedersMeetTheTrunk() {
		List<int[]> lens = ResonanceTableLayout.trace(ResonanceTableLayout.FEEDER_FROM_LENS);
		List<int[]> amethyst = ResonanceTableLayout.trace(ResonanceTableLayout.FEEDER_FROM_AMETHYST);
		List<int[]> trunk = ResonanceTableLayout.trace(ResonanceTableLayout.TRUNK_TO_DIAL);

		assertEquals(ResonanceTableLayout.LENS_SLOT_X + ResonanceTableLayout.SLOT_SIZE + 3,
				lens.getFirst()[0], "the lens feeder does not start beside the lens slot");
		assertEquals(ResonanceTableLayout.AMETHYST_SLOT_X + ResonanceTableLayout.SLOT_SIZE + 3,
				amethyst.getFirst()[0], "the amethyst feeder does not start beside its slot");

		int[] junction = trunk.getFirst();
		assertArrayEqualsish(junction, lens.getLast(), "the lens feeder does not reach the junction");
		assertArrayEqualsish(junction, amethyst.getLast(),
				"the amethyst feeder does not reach the junction");
	}

	@Test
	@DisplayName("the trunk meets the dial's rim without crossing into it")
	void trunkStopsAtTheDial() {
		List<int[]> trunk = ResonanceTableLayout.trace(ResonanceTableLayout.TRUNK_TO_DIAL);
		int[] end = trunk.getLast();
		double distance = Math.hypot(end[0] - ResonanceTableLayout.DIAL_CENTRE_X,
				end[1] - ResonanceTableLayout.DIAL_CENTRE_Y);
		assertTrue(distance > ResonanceTableLayout.RING_SPRITE_SIZE / 2.0 - 8,
				"the trunk ends " + distance + " from the centre, inside the dial");
		assertTrue(distance <= ResonanceTableLayout.RING_SPRITE_SIZE / 2.0,
				"the trunk reaches past the dial's rim at " + distance);
	}

	@Test
	@DisplayName("the rotating ring sprite sits inside the texture and clears the panel")
	void ringSpriteFits() {
		assertTrue(ResonanceTableLayout.RING_SPRITE_U >= ResonanceTableLayout.PANEL_WIDTH,
				"the ring sprite overlaps the panel it is drawn over");
		assertTrue(ResonanceTableLayout.RING_SPRITE_U + ResonanceTableLayout.RING_SPRITE_SIZE <= 256,
				"the ring sprite runs off the texture");
		assertTrue(ResonanceTableLayout.RING_SPRITE_V + ResonanceTableLayout.RING_SPRITE_SIZE <= 256,
				"the ring sprite runs off the texture");
	}

	private static void assertArrayEqualsish(int[] expected, int[] actual, String message) {
		List<Integer> a = new ArrayList<>(List.of(expected[0], expected[1]));
		List<Integer> b = new ArrayList<>(List.of(actual[0], actual[1]));
		assertEquals(a, b, message);
	}
}
