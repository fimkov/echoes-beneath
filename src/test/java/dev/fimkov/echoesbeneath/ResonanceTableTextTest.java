package dev.fimkov.echoesbeneath;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.fimkov.echoesbeneath.menu.ResonanceTableLayout;
import dev.fimkov.echoesbeneath.menu.ResonanceTableUpgrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResonanceTableTextTest {
	private static final String[] LANGUAGES = {"en_us", "ru_ru"};
	private static final int FALLBACK_ADVANCE = 6;

	private static final Map<Integer, Integer> ADVANCES = loadAdvances();

	private static Map<Integer, Integer> loadAdvances() {
		Map<Integer, Integer> advances = new HashMap<>();

		try (InputStream stream = ResonanceTableTextTest.class.getResourceAsStream("/echo_font_advances.txt")) {
			assertNotNull(stream, "the font advance table is missing");
			Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8);

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();

				if (line.isEmpty()) {
					continue;
				}
				String[] parts = line.split("=");
				advances.put(Integer.parseInt(parts[0], 16), Integer.parseInt(parts[1]));
			}
		} catch (Exception failure) {
			throw new IllegalStateException(failure);
		}

		return advances;
	}

	private static JsonObject language(String code) {
		try (InputStream stream = ResonanceTableTextTest.class

				.getResourceAsStream("/assets/echoes_beneath/lang/" + code + ".json")) {
			assertNotNull(stream, code + " is missing");

			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception failure) {
			throw new IllegalStateException(failure);
		}
	}

	private static int width(String text) {
		int total = 0;

		for (int index = 0; index < text.length(); index++) {
			total += ADVANCES.getOrDefault((int) text.charAt(index), FALLBACK_ADVANCE);
		}

		return total;
	}

	private static String translate(JsonObject language, String key, Object... arguments) {
		assertTrue(language.has(key), "missing translation key " + key);
		String value = language.get(key).asString().orElse("");

		for (Object argument : arguments) {
			value = value.replaceFirst("%s", String.valueOf(argument));
		}

		return value;
	}

	@Test
	@DisplayName("upgrade names fit between the icon and the meter in every language")
	void upgradeNamesFitTheirRow() {
		List<String> failures = new ArrayList<>();

		for (String code : LANGUAGES) {
			JsonObject language = language(code);

			for (ResonanceTableUpgrade upgrade : ResonanceTableUpgrade.all()) {
				String label = translate(language, upgrade.nameKey());
				int budget = ResonanceTableLayout.labelWidthBudget(upgrade.maxSteps());

				if (width(label) > budget) {
					failures.add(code + " " + upgrade.nameKey() + " is " + width(label)
							+ "px, budget is " + budget + "px");
				}
			}
		}

		assertTrue(failures.isEmpty(), String.join("; ", failures));
	}

	@Test
	@DisplayName("the cost readout never reaches back into the dial")
	void costTextFitsItsColumn() {
		int budget = ResonanceTableLayout.COST_RIGHT - ResonanceTableLayout.COST_LEFT_LIMIT;
		List<String> failures = new ArrayList<>();

		for (String code : LANGUAGES) {
			JsonObject language = language(code);
			List<String> samples = new ArrayList<>();
			samples.add(translate(language, EchoTranslations.UPGRADE_LEVELS));
			samples.add("888");

			for (String sample : samples) {
				if (width(sample) > budget) {
					failures.add(code + " \"" + sample + "\" is " + width(sample) + "px, budget is " + budget + "px");
				}
			}
			int shardLine = width("99") + 2 + ResonanceTableLayout.SHARD_ICON_SIZE;

			if (shardLine > budget) {
				failures.add(code + " shard line is " + shardLine + "px, budget is " + budget + "px");
			}
		}

		assertTrue(failures.isEmpty(), String.join("; ", failures));
	}

	@Test
	@DisplayName("vanilla things are called by the names the game gives them")
	void vanillaNamesMatchTheGame() {
		Map<String, String> wrongRussian = Map.of(
				"эхо-осколок", "осколок эха",
				"эхо-осколк", "осколок эха",
				"аметистовый осколок", "осколок аметиста",
				"аметистовых осколков", "осколков аметиста",
				"зачарования", "чародейский стол",
				"зачаровывания", "чародейский стол",
				"варден", "Хранитель",
				"глубинная плитка", "глубинносланцевый плитняк");
		JsonObject russian = language("ru_ru");
		List<String> wrong = new ArrayList<>();

		russian.entrySet().forEach(entry -> {
			String value = entry.getValue().asString().orElse("").toLowerCase(Locale.ROOT);
			wrongRussian.forEach((bad, good) -> {
				if (value.contains(bad)) {
					wrong.add(entry.getKey() + " says \"" + bad + "\", the game says \"" + good + "\"");
				}
			});
		});

		assertTrue(wrong.isEmpty(), String.join("; ", wrong));
	}

	@Test
	@DisplayName("the two languages describe the same set of keys")
	void languagesAgreeOnKeys() {
		JsonObject english = language("en_us");
		JsonObject russian = language("ru_ru");
		List<String> missing = new ArrayList<>();

		english.keySet().forEach(key -> {
			if (!russian.has(key)) {
				missing.add("ru_ru is missing " + key);
			}
		});

		russian.keySet().forEach(key -> {
			if (!english.has(key)) {
				missing.add("en_us is missing " + key);
			}
		});

		assertTrue(missing.isEmpty(), String.join("; ", missing));
	}

	@Test
	@DisplayName("no translation the table shows is left empty")
	void tableTranslationsArePresent() {
		for (String code : LANGUAGES) {
			JsonObject language = language(code);

			for (ResonanceTableUpgrade upgrade : ResonanceTableUpgrade.all()) {
				assertFalse(translate(language, upgrade.nameKey()).isBlank(), code + " " + upgrade.nameKey());
				assertFalse(translate(language, upgrade.unitKey(), 1).isBlank(), code + " " + upgrade.unitKey());
			}
		}
	}
}
