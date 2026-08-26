package dev.fimkov.echoesbeneath;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.fimkov.echoesbeneath.block.EchoTableShapes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EchoBlockShapeTest {
	private static final Map<String, double[][]> DECLARED = Map.of(
			"resonance_table", EchoTableShapes.RESONANCE_BOXES,
			"sounding_table", EchoTableShapes.SOUNDING_BOXES);

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

	private static List<String> boxesOf(double[][] boxes) {
		TreeSet<String> sorted = new TreeSet<>();
		for (double[] box : boxes) {
			sorted.add(describe(box[0], box[1], box[2], box[3], box[4], box[5]));
		}
		return new ArrayList<>(sorted);
	}

	private static String describe(double... values) {
		StringBuilder text = new StringBuilder();
		for (double value : values) {
			text.append(Math.round(value)).append(' ');
		}
		return text.toString();
	}

	@Test
	@DisplayName("what you bump into is exactly what is drawn")
	void outlinesMatchTheirModels() {
		for (Map.Entry<String, double[][]> entry : DECLARED.entrySet()) {
			JsonArray elements = model(entry.getKey()).getAsJsonArray("elements");
			TreeSet<String> drawn = new TreeSet<>();
			for (int index = 0; index < elements.size(); index++) {
				JsonObject element = elements.get(index).getAsJsonObject();
				JsonArray from = element.getAsJsonArray("from");
				JsonArray to = element.getAsJsonArray("to");
				drawn.add(describe(from.get(0).getAsDouble(), from.get(1).getAsDouble(),
						from.get(2).getAsDouble(), to.get(0).getAsDouble(),
						to.get(1).getAsDouble(), to.get(2).getAsDouble()));
			}
			assertEquals(new ArrayList<>(drawn), boxesOf(entry.getValue()),
					entry.getKey() + " is outlined differently from how it is drawn");
		}
	}
}
