package dev.fimkov.echoesbeneath;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.fimkov.echoesbeneath.item.EchoLensAppearance;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.memory.EchoFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

	private record State(int vision, int capacity, int filled, int radiusTier, int hearingTier) {
	}

	private static List<State> everyState() {
		List<State> states = new ArrayList<>();
		for (int vision = 0; vision <= EchoLensUpgrades.MAX_VISION; vision++) {
			for (int capacity = EchoFormat.DEFAULT_MEMORY_SLOTS;
					capacity <= EchoFormat.MAX_MEMORY_SLOTS; capacity++) {
				for (int filled = 0; filled <= capacity; filled++) {
					for (int radius = 0; radius < EchoLensAppearance.RADIUS_TIERS; radius++) {
						for (int hearing = 0; hearing < EchoLensAppearance.HEARING_TIERS; hearing++) {
							states.add(new State(vision, capacity, filled, radius, hearing));
						}
					}
				}
			}
		}
		return states;
	}

	private static int indexOf(State state) {
		return EchoLensAppearance.stateIndex(state.vision(), state.capacity(), state.filled(),
				radiusForTier(state.radiusTier()), state.hearingTier());
	}

	private static float valueOf(State state) {
		return EchoLensAppearance.tuningValue(state.vision(), state.capacity(), state.filled(),
				radiusForTier(state.radiusTier()), state.hearingTier());
	}

	private static int radiusForTier(int tier) {
		return EchoLensUpgrades.MIN_RADIUS + tier * EchoLensUpgrades.RADIUS_STEP;
	}

	@Test
	@DisplayName("every lens state has its own index, in order and without gaps")
	void indicesAreDenseAndOrdered() {
		List<State> states = everyState();
		assertEquals(EchoLensAppearance.STATE_COUNT, states.size());
		for (int expected = 0; expected < states.size(); expected++) {
			State state = states.get(expected);
			assertEquals(expected, indexOf(state), "state " + state + " landed on the wrong index");
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
			String path = override.get("model").getAsString().replace("echoes_beneath:item/", "");
			JsonObject child = asset("/assets/echoes_beneath/models/item/" + path + ".json");
			JsonObject textures = child.getAsJsonObject("textures");
			String[] expectedLayers = {
					"echoes_beneath:item/echo_lens_shell_" + state.radiusTier(),
					"echoes_beneath:item/echo_lens_core_" + state.capacity() + "_" + state.filled(),
					"echoes_beneath:item/echo_lens_eyes_" + state.vision(),
					"echoes_beneath:item/echo_lens_ears_" + state.hearingTier(),
			};
			for (int layer = 0; layer < expectedLayers.length; layer++) {
				String actual = textures.get("layer" + layer).getAsString();
				if (!expectedLayers[layer].equals(actual)) {
					wrong.add(state + " layer" + layer + " is " + actual);
				}
			}
		}
		assertTrue(wrong.isEmpty(), String.join("; ", wrong));
	}

	private static BufferedImage texture(String name) throws IOException {
		try (InputStream stream = EchoLensAppearanceTest.class
				.getResourceAsStream("/assets/echoes_beneath/textures/item/" + name + ".png")) {
			assertNotNull(stream, name + " is missing");
			return ImageIO.read(stream);
		}
	}

	@Test
	@DisplayName("the plain item texture is exactly the untouched lens, layer for layer")
	void plainTextureIsTheUntouchedComposite() throws IOException {
		BufferedImage plain = texture("echo_lens");
		BufferedImage[] layers = {
				texture("echo_lens_shell_0"),
				texture("echo_lens_core_1_0"),
				texture("echo_lens_eyes_0"),
				texture("echo_lens_ears_0"),
		};
		List<String> wrong = new ArrayList<>();
		for (int y = 0; y < plain.getHeight(); y++) {
			for (int x = 0; x < plain.getWidth(); x++) {
				int composed = 0;
				for (BufferedImage layer : layers) {
					int pixel = layer.getRGB(x, y);
					if ((pixel >>> 24) != 0) {
						composed = pixel;
					}
				}
				if (composed != plain.getRGB(x, y) && wrong.size() < 8) {
					wrong.add(x + "," + y);
				}
			}
		}
		assertTrue(wrong.isEmpty(), "the layers do not rebuild the plain lens at " + String.join(" ", wrong));
	}

	@Test
	@DisplayName("every state picks its own model and no other")
	void everyStateSelectsItsOwnModel() {
		JsonArray overrides = asset("/assets/echoes_beneath/models/item/echo_lens.json")
				.getAsJsonArray("overrides");
		List<State> states = everyState();
		List<String> wrong = new ArrayList<>();
		for (int index = 0; index < states.size(); index++) {
			float value = valueOf(states.get(index));
			int chosen = 0;
			for (int candidate = 0; candidate < overrides.size(); candidate++) {
				float threshold = overrides.get(candidate).getAsJsonObject()
						.getAsJsonObject("predicate").get("echoes_beneath:tuning").getAsFloat();
				if (value >= threshold) {
					chosen = candidate;
				}
			}
			if (chosen != index && wrong.size() < 8) {
				wrong.add(index + " picked " + chosen);
			}
		}
		assertTrue(wrong.isEmpty(), String.join("; ", wrong));
	}
}
