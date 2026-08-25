package dev.fimkov.echoesbeneath;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.fimkov.echoesbeneath.item.EchoLensAppearance;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.memory.EchoFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoLensAppearanceTest {
	private static JsonObject asset(String path) {
		try (InputStream stream = EchoLensAppearanceTest.class.getResourceAsStream(path)) {
			assertNotNull(stream, path + " is missing");
			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception failure) {
			throw new IllegalStateException(path, failure);
		}
	}

	private record State(int vision, int capacity, int filled) {
	}

	private static List<State> everyState() {
		List<State> states = new ArrayList<>();
		for (int vision = 0; vision <= EchoLensUpgrades.MAX_VISION; vision++) {
			for (int capacity = EchoFormat.DEFAULT_MEMORY_SLOTS;
					capacity <= EchoFormat.MAX_MEMORY_SLOTS; capacity++) {
				for (int filled = 0; filled <= capacity; filled++) {
					states.add(new State(vision, capacity, filled));
				}
			}
		}
		return states;
	}

	@Test
	@DisplayName("every lens state has its own index, in order and without gaps")
	void indicesAreDenseAndOrdered() {
		List<State> states = everyState();
		assertEquals(EchoLensAppearance.STATE_COUNT, states.size());
		for (int expected = 0; expected < states.size(); expected++) {
			State state = states.get(expected);
			assertEquals(expected,
					EchoLensAppearance.stateIndex(state.vision(), state.capacity(), state.filled()),
					"state " + state + " landed on the wrong index");
		}
	}

	@Test
	@DisplayName("the model overrides cover every state and point at the matching textures")
	void modelOverridesMatchTheStates() {
		JsonObject model = asset("/assets/echoes_beneath/models/item/echo_lens.json");
		JsonArray overrides = model.getAsJsonArray("overrides");
		assertNotNull(overrides, "the lens model declares no overrides");
		assertEquals(EchoLensAppearance.STATE_COUNT, overrides.size());
		List<State> states = everyState();
		List<String> wrong = new ArrayList<>();
		for (int index = 0; index < overrides.size(); index++) {
			State state = states.get(index);
			JsonObject override = overrides.get(index).getAsJsonObject();
			float declared = override.getAsJsonObject("predicate")
					.get("echoes_beneath:tuning").getAsFloat();
			float expected = EchoLensAppearance.tuningValue(state.vision(), state.capacity(), state.filled());
			if (Math.abs(declared - expected) > 1.0E-4F) {
				wrong.add(state + " predicate " + declared + " but the item reports " + expected);
				continue;
			}
			String path = override.get("model").getAsString().replace("echoes_beneath:item/", "");
			JsonObject child = asset("/assets/echoes_beneath/models/item/" + path + ".json");
			JsonObject textures = child.getAsJsonObject("textures");
			String body = "echoes_beneath:item/echo_lens_v" + state.vision();
			String marks = "echoes_beneath:item/echo_lens_marks_" + state.capacity() + "_" + state.filled();
			if (!body.equals(textures.get("layer0").getAsString())) {
				wrong.add(state + " uses body " + textures.get("layer0").getAsString());
			}
			if (!marks.equals(textures.get("layer1").getAsString())) {
				wrong.add(state + " uses marks " + textures.get("layer1").getAsString());
			}
		}
		assertTrue(wrong.isEmpty(), String.join("; ", wrong));
	}

	@Test
	@DisplayName("an untouched lens looks exactly like the plain item texture")
	void visionZeroIsThePlainLens() throws Exception {
		byte[] plain;
		byte[] visionZero;
		try (InputStream a = EchoLensAppearanceTest.class
				.getResourceAsStream("/assets/echoes_beneath/textures/item/echo_lens.png");
				InputStream b = EchoLensAppearanceTest.class
						.getResourceAsStream("/assets/echoes_beneath/textures/item/echo_lens_v0.png")) {
			assertNotNull(a, "the fallback lens texture is missing");
			assertNotNull(b, "the vision 0 lens texture is missing");
			plain = a.readAllBytes();
			visionZero = b.readAllBytes();
		}
		assertArrayEquals(plain, visionZero, "vision 0 has drifted away from the plain lens");
	}

	@Test
	@DisplayName("neighbouring states stay far enough apart to select cleanly")
	void statesAreDistinguishable() {
		float step = 1.0F / (EchoLensAppearance.STATE_COUNT - 1);
		assertTrue(step > 1.0E-3F, "states are only " + step + " apart");
	}
}
