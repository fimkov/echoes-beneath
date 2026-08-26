package dev.fimkov.echoesbeneath;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EchoBlockShapeTest {
	private static final Map<String, Double> DECLARED_HEIGHT = Map.of(
			"resonance_table", 12.0D,
			"sounding_table", 13.0D);

	private static JsonObject model(String name) {
		try (InputStream stream = EchoBlockShapeTest.class
				.getResourceAsStream("/assets/echoes_beneath/models/block/" + name + ".json")) {
			assertNotNull(stream, name + " has no model");
			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
					.getAsJsonObject();
		} catch (Exception failure) {
			throw new IllegalStateException(failure);
		}
	}

	@Test
	@DisplayName("what you bump into is as tall as what you can see")
	void outlinesMatchTheirModels() {
		for (Map.Entry<String, Double> entry : DECLARED_HEIGHT.entrySet()) {
			JsonArray elements = model(entry.getKey()).getAsJsonArray("elements");
			double tallest = 0.0D;
			for (int index = 0; index < elements.size(); index++) {
				double top = elements.get(index).getAsJsonObject()
						.getAsJsonArray("to").get(1).getAsDouble();
				tallest = Math.max(tallest, top);
			}
			assertEquals(entry.getValue(), tallest, entry.getKey()
					+ " is drawn " + tallest + " tall but its outline says " + entry.getValue());
		}
	}
}
