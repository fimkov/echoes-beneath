package dev.fimkov.echoesbeneath;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoAdvancementTreeTest {
	private static final Path ADVANCEMENTS =
			Path.of("src/main/generated/data/echoes_beneath/advancement");
	private static final String NAMESPACE = "echoes_beneath:";

	private static Map<String, JsonObject> tree() {
		Map<String, JsonObject> found = new HashMap<>();

		try (Stream<Path> files = Files.list(ADVANCEMENTS)) {
			files.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
				try {
					String name = path.getFileName().toString().replace(".json", "");
					found.put(NAMESPACE + name,
							JsonParser.parseString(Files.readString(path)).getAsJsonObject());
				} catch (IOException failure) {
					throw new IllegalStateException(path.toString(), failure);
				}
			});
		} catch (IOException failure) {
			throw new IllegalStateException(failure);
		}

		return found;
	}

	private static JsonObject language(String code) {
		try (var stream = EchoAdvancementTreeTest.class

				.getResourceAsStream("/assets/echoes_beneath/lang/" + code + ".json")) {
			assertNotNull(stream, code + " is missing");

			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception failure) {
			throw new IllegalStateException(failure);
		}
	}

	@Test
	@DisplayName("every advancement points at a parent that exists")
	void parentsResolve() {
		Map<String, JsonObject> tree = tree();
		assertFalse(tree.isEmpty(), "no advancements were generated");
		List<String> broken = new ArrayList<>();

		for (Map.Entry<String, JsonObject> entry : tree.entrySet()) {
			if (!entry.getValue().has("parent")) {
				continue;
			}
			String parent = entry.getValue().get("parent").getAsString();

			if (parent.startsWith(NAMESPACE) && !tree.containsKey(parent)) {
				broken.add(entry.getKey() + " points at missing " + parent);
			}
		}

		assertTrue(broken.isEmpty(), String.join("; ", broken));
	}

	@Test
	@DisplayName("exactly one advancement is a root")
	void oneRoot() {
		List<String> roots = tree().entrySet().stream()
				.filter(entry -> !entry.getValue().has("parent"))
				.map(Map.Entry::getKey)
				.toList();
		assertTrue(roots.size() == 1, "roots found: " + roots);
	}

	@Test
	@DisplayName("every advancement title and description is translated in both languages")
	void titlesAreTranslated() {
		JsonObject english = language("en_us");
		JsonObject russian = language("ru_ru");
		List<String> missing = new ArrayList<>();

		for (Map.Entry<String, JsonObject> entry : tree().entrySet()) {
			JsonObject display = entry.getValue().getAsJsonObject("display");
			assertNotNull(display, entry.getKey() + " has no display block");

			for (String part : new String[] {"title", "description"}) {
				String key = display.getAsJsonObject(part).get("translate").getAsString();

				if (!english.has(key)) {
					missing.add("en_us " + key);
				}
				if (!russian.has(key)) {
					missing.add("ru_ru " + key);
				}
			}
		}

		assertTrue(missing.isEmpty(), String.join("; ", missing));
	}
}
