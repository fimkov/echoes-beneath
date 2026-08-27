package dev.fimkov.echoesbeneath;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.fimkov.echoesbeneath.menu.SoundingTableLayout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundingTableTextTest {
	private static final String[] LANGUAGES = {"en_us", "ru_ru"};
	private static final int ROW_PADDING = 5;
	private static final int ROW_GAP = 4;
	private static final int BUTTON_PADDING = 4;
	private static final int STAGE_PADDING = 8;
	private static final String WIDEST_NUMERAL = "III";
	private static final String WIDEST_CLOCK = "02:00";
	private static final String WIDEST_WINDOW_LOW = "-64";
	private static final String WIDEST_WINDOW_HIGH = "319";

	private static JsonObject language(String code) {
		try (InputStream stream = SoundingTableTextTest.class

				.getResourceAsStream("/assets/echoes_beneath/lang/" + code + ".json")) {
			assertNotNull(stream, code + " is missing");

			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
					.getAsJsonObject();
		} catch (Exception failure) {
			throw new IllegalStateException(failure);
		}
	}

	@Test
	@DisplayName("a memory row holds its numeral and its clock without them touching")
	void rowsFit() {
		Map<Integer, Integer> advances = advances();
		int used = ROW_PADDING + measure(advances, WIDEST_NUMERAL) + ROW_GAP
				+ measure(advances, WIDEST_CLOCK) + ROW_PADDING;
		assertTrue(used <= SoundingTableLayout.LIST_WIDTH,
				"a row needs " + used + "px against " + SoundingTableLayout.LIST_WIDTH);
	}

	@Test
	@DisplayName("the button and the height readout fit in every language")
	void labelsFit() {
		Map<Integer, Integer> advances = advances();
		List<String> tooWide = new ArrayList<>();

		for (String code : LANGUAGES) {
			JsonObject language = language(code);

			for (String key : new String[] {"sounding.echoes_beneath.extract",
					"sounding.echoes_beneath.put_back"}) {
				int button = measure(advances, language.get(key).asString().orElse("")) + BUTTON_PADDING * 2;

				if (button > SoundingTableLayout.EXTRACT_WIDTH) {
					tooWide.add(code + " " + key + " is " + button + "px against "
							+ SoundingTableLayout.EXTRACT_WIDTH);
				}
			}
			String window = language.get("sounding.echoes_beneath.window").asString().orElse("")
					.replace("%s", "")
					+ WIDEST_WINDOW_LOW + WIDEST_WINDOW_HIGH;
			int readout = measure(advances, window) + STAGE_PADDING;

			if (readout > SoundingTableLayout.STAGE_WIDTH) {
				tooWide.add(code + " window is " + readout + "px against "
						+ SoundingTableLayout.STAGE_WIDTH);
			}
			for (String key : new String[] {"sounding.echoes_beneath.no_lens",
					"sounding.echoes_beneath.no_memories"}) {
				int width = measure(advances, language.get(key).asString().orElse("")) + STAGE_PADDING;

				if (width > SoundingTableLayout.STAGE_WIDTH) {
					tooWide.add(code + " " + key + " is " + width + "px against "
							+ SoundingTableLayout.STAGE_WIDTH);
				}
			}
		}

		assertTrue(tooWide.isEmpty(), String.join("; ", tooWide));
	}

	@Test
	@DisplayName("nothing on the sounding table overlaps anything else")
	void widgetsDoNotOverlap() {
		List<int[]> boxes = List.of(
				new int[] {SoundingTableLayout.LENS_SLOT_X - 4, SoundingTableLayout.LENS_SLOT_Y - 4, 24, 24},
				new int[] {SoundingTableLayout.RESULT_SLOT_X - 4, SoundingTableLayout.RESULT_SLOT_Y - 4, 24, 24},
				new int[] {SoundingTableLayout.LIST_X, SoundingTableLayout.LIST_Y,
						SoundingTableLayout.LIST_WIDTH,
						SoundingTableLayout.ROW_COUNT * (SoundingTableLayout.ROW_HEIGHT
								+ SoundingTableLayout.ROW_GAP) - SoundingTableLayout.ROW_GAP},
				new int[] {SoundingTableLayout.STAGE_X, SoundingTableLayout.STAGE_Y,
						SoundingTableLayout.STAGE_WIDTH, SoundingTableLayout.STAGE_HEIGHT},
				new int[] {SoundingTableLayout.RULER_X, SoundingTableLayout.RULER_Y,
						SoundingTableLayout.RULER_WIDTH, SoundingTableLayout.RULER_HEIGHT},
				new int[] {SoundingTableLayout.EXTRACT_X, SoundingTableLayout.EXTRACT_Y,
						SoundingTableLayout.EXTRACT_WIDTH, SoundingTableLayout.EXTRACT_HEIGHT},
				new int[] {SoundingTableLayout.INVENTORY_X, SoundingTableLayout.INVENTORY_Y, 162, 54},
				new int[] {SoundingTableLayout.INVENTORY_X, SoundingTableLayout.HOTBAR_Y, 162, 18});
		List<String> clashes = new ArrayList<>();

		for (int first = 0; first < boxes.size(); first++) {
			int[] a = boxes.get(first);

			if (a[0] < 0 || a[1] < 0 || a[0] + a[2] > SoundingTableLayout.PANEL_WIDTH
					|| a[1] + a[3] > SoundingTableLayout.PANEL_HEIGHT) {
				clashes.add("box " + first + " leaves the panel");
			}
			for (int second = first + 1; second < boxes.size(); second++) {
				int[] b = boxes.get(second);

				if (a[0] < b[0] + b[2] && b[0] < a[0] + a[2]
						&& a[1] < b[1] + b[3] && b[1] < a[1] + a[3]) {
					clashes.add("box " + first + " overlaps box " + second);
				}
			}
		}

		assertTrue(clashes.isEmpty(), String.join("; ", clashes));
	}

	private static Map<Integer, Integer> advances() {
		Map<Integer, Integer> table = new HashMap<>();

		try (InputStream stream = SoundingTableTextTest.class

				.getResourceAsStream("/echo_font_advances.txt")) {
			assertNotNull(stream, "the font advance table is missing");
			Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8);

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();

				if (line.isEmpty()) {
					continue;
				}
				String[] parts = line.split("=");
				table.put(Integer.parseInt(parts[0], 16), Integer.parseInt(parts[1]));
			}
		} catch (Exception failure) {
			throw new IllegalStateException(failure);
		}

		return table;
	}

	private static int measure(Map<Integer, Integer> advances, String text) {
		int total = 0;

		for (int index = 0; index < text.length(); index++) {
			total += advances.getOrDefault((int) text.charAt(index), 6);
		}

		return total;
	}
}
