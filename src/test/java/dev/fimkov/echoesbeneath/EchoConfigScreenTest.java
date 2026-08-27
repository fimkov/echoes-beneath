package dev.fimkov.echoesbeneath;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.fimkov.echoesbeneath.client.config.EchoClientConfigValues;
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

class EchoConfigScreenTest {
	private record Option(String key, String widestValue) {
	}

	private static final Option[] OPTIONS = {
			new Option("wave_budget", ": 160000"),
			new Option("wave_detail", ": 16"),
			new Option("hologram_budget", ": 4096"),
			new Option("hologram_distance", ": 96"),
			new Option("transition", ": 200"),
			new Option("ambience", ": 100"),
			new Option("show_holograms", ": OFF"),
			new Option("hide_sky", ": OFF"),
			new Option("darkness", ": 100"),
			new Option("wave_volume", ": 100"),
			new Option("chart_budget", ": 20000"),
			new Option("chart_distance", ": 128"),
			new Option("chart_aura", ": OFF"),
			new Option("table_animations", ": OFF"),
	};
	private static final int WIDGET_WIDTH = 150;
	private static final int LABEL_BUDGET = WIDGET_WIDTH - 8;

	private static JsonObject language(String code) {
		try (InputStream stream = EchoConfigScreenTest.class

				.getResourceAsStream("/assets/echoes_beneath/lang/" + code + ".json")) {
			assertNotNull(stream, code + " is missing");

			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
					.getAsJsonObject();
		} catch (Exception failure) {
			throw new IllegalStateException(failure);
		}
	}

	@Test
	@DisplayName("every option on the settings screen is named in both languages")
	void optionsAreNamed() {
		JsonObject english = language("en_us");
		JsonObject russian = language("ru_ru");
		List<String> missing = new ArrayList<>();

		for (Option option : OPTIONS) {
			String key = "config.echoes_beneath." + option.key();

			if (!english.has(key)) {
				missing.add("en_us " + key);
			}
			if (!russian.has(key)) {
				missing.add("ru_ru " + key);
			}
		}

		for (String key : new String[] {"config.echoes_beneath.title",
				"key.echoes_beneath.open_config", "key.categories.echoes_beneath"}) {
			if (!english.has(key)) {
				missing.add("en_us " + key);
			}
			if (!russian.has(key)) {
				missing.add("ru_ru " + key);
			}
		}

		assertTrue(missing.isEmpty(), String.join("; ", missing));
	}

	@Test
	@DisplayName("no option label plus its value can outgrow the widget it sits in")
	void labelsFitTheirWidgets() {
		Map<Integer, Integer> advances = advances();
		List<String> tooWide = new ArrayList<>();

		for (String code : new String[] {"en_us", "ru_ru"}) {
			JsonObject language = language(code);

			for (Option option : OPTIONS) {
				String label = language.get("config.echoes_beneath." + option.key()).asString().orElse("");
				int width = measure(advances, label + option.widestValue());

				if (width > LABEL_BUDGET) {
					tooWide.add(code + " " + option.key() + " is " + width
							+ "px against " + LABEL_BUDGET);
				}
			}
		}

		assertTrue(tooWide.isEmpty(), String.join("; ", tooWide));
	}

	private static Map<Integer, Integer> advances() {
		Map<Integer, Integer> table = new HashMap<>();

		try (InputStream stream = EchoConfigScreenTest.class

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

	@Test
	@DisplayName("the screen covers every client setting the file holds")
	void screenCoversTheWholeFile() {
		assertTrue(OPTIONS.length == EchoClientConfigValues.class.getRecordComponents().length,
				"the screen shows " + OPTIONS.length + " of "
						+ EchoClientConfigValues.class.getRecordComponents().length + " client settings");
	}
}
