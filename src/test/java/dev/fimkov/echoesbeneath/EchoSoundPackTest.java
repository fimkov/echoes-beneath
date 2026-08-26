package dev.fimkov.echoesbeneath;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.sounds.SoundEvent;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EchoSoundPackTest {
	private static final String NAMESPACE = "echoes_beneath";
	private static final byte[] OGG_MAGIC = {'O', 'g', 'g', 'S'};

	private static JsonObject asset(String path) {
		try (InputStream stream = EchoSoundPackTest.class.getResourceAsStream(path)) {
			assertNotNull(stream, path + " is missing");
			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception failure) {
			throw new IllegalStateException(path, failure);
		}
	}

	private static JsonObject definitions() {
		return asset("/assets/" + NAMESPACE + "/sounds.json");
	}

	@Test
	@DisplayName("every declared sound has a real ogg file behind it")
	void everySoundHasAFile() {
		List<String> missing = new ArrayList<>();
		for (Map.Entry<String, com.google.gson.JsonElement> entry : definitions().entrySet()) {
			for (var element : entry.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
				String name = element.getAsJsonObject().get("name").getAsString()
						.replace(NAMESPACE + ":", "");
				String path = "/assets/" + NAMESPACE + "/sounds/" + name + ".ogg";
				try (InputStream stream = EchoSoundPackTest.class.getResourceAsStream(path)) {
					if (stream == null) {
						missing.add(path);
						continue;
					}
					byte[] header = stream.readNBytes(OGG_MAGIC.length);
					for (int index = 0; index < OGG_MAGIC.length; index++) {
						if (header.length <= index || header[index] != OGG_MAGIC[index]) {
							missing.add(path + " is not an Ogg stream");
							break;
						}
					}
				} catch (Exception failure) {
					missing.add(path + " could not be read");
				}
			}
		}
		assertTrue(missing.isEmpty(), String.join("; ", missing));
	}

	@Test
	@DisplayName("every sound is subtitled in both languages")
	void everySoundIsSubtitled() {
		JsonObject english = asset("/assets/" + NAMESPACE + "/lang/en_us.json");
		JsonObject russian = asset("/assets/" + NAMESPACE + "/lang/ru_ru.json");
		List<String> missing = new ArrayList<>();
		for (Map.Entry<String, com.google.gson.JsonElement> entry : definitions().entrySet()) {
			JsonObject definition = entry.getValue().getAsJsonObject();
			assertTrue(definition.has("subtitle"), entry.getKey() + " has no subtitle");
			String key = definition.get("subtitle").getAsString();
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
	@DisplayName("every registered sound event is named by the pack, under the key the game looks up")
	void registeredEventsAreDeclared() throws IllegalAccessException {
		JsonObject definitions = definitions();
		List<String> missing = new ArrayList<>();
		for (java.lang.reflect.Field field : EchoSounds.class.getFields()) {
			if (!SoundEvent.class.isAssignableFrom(field.getType())) {
				continue;
			}
			SoundEvent event =
					(SoundEvent) field.get(null);
			String key = event.getLocation().getPath();
			if (!event.getLocation().getNamespace().equals(NAMESPACE)) {
				continue;
			}
			if (!definitions.has(key)) {
				missing.add(field.getName() + " is registered as " + event.getLocation()
						+ " but the pack has no entry named " + key);
			}
		}
		assertTrue(missing.isEmpty(), String.join("; ", missing));
	}

	@Test
	@DisplayName("the sound pack is not empty and every entry names a category")
	void definitionsAreComplete() {
		JsonObject definitions = definitions();
		assertFalse(definitions.isEmpty(), "no sounds are declared");
		for (Map.Entry<String, com.google.gson.JsonElement> entry : definitions.entrySet()) {
			assertTrue(entry.getValue().getAsJsonObject().has("category"),
					entry.getKey() + " declares no category");
		}
	}
}
