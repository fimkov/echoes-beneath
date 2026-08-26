package dev.fimkov.echoesbeneath;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoAnimatedTextureTest {
	private static final Path TEXTURES = Path.of("src/main/resources/assets/echoes_beneath/textures");

	private static List<Path> animated() {
		try (Stream<Path> files = Files.walk(TEXTURES)) {
			return files.filter(path -> path.toString().endsWith(".png.mcmeta")).sorted().toList();
		} catch (IOException failure) {
			throw new IllegalStateException(failure);
		}
	}

	@Test
	@DisplayName("every animated texture is a whole number of square frames")
	void stripsDivideEvenly() throws IOException {
		List<Path> metas = animated();
		assertTrue(!metas.isEmpty(), "no animated textures were found at all");
		List<String> wrong = new ArrayList<>();

		for (Path meta : metas) {
			Path image = meta.resolveSibling(meta.getFileName().toString().replace(".mcmeta", ""));
			assertTrue(Files.isRegularFile(image), image + " has a mcmeta but no texture");
			BufferedImage picture;

			try (InputStream stream = Files.newInputStream(image)) {
				picture = ImageIO.read(stream);
			}
			assertNotNull(picture, image + " is not readable");

			if (picture.getHeight() % picture.getWidth() != 0) {
				wrong.add(image.getFileName() + " is " + picture.getWidth() + "x" + picture.getHeight());
			}
		}

		assertTrue(wrong.isEmpty(), String.join("; ", wrong));
	}

	@Test
	@DisplayName("every animation names frames the strip actually contains")
	void framesExist() throws IOException {
		List<String> wrong = new ArrayList<>();

		for (Path meta : animated()) {
			Path image = meta.resolveSibling(meta.getFileName().toString().replace(".mcmeta", ""));
			BufferedImage picture;

			try (InputStream stream = Files.newInputStream(image)) {
				picture = ImageIO.read(stream);
			}
			int frames = picture.getHeight() / picture.getWidth();
			JsonObject animation;

			try (InputStream stream = Files.newInputStream(meta)) {
				animation = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
						.getAsJsonObject().getAsJsonObject("animation");
			}
			assertNotNull(animation, meta + " declares no animation");

			if (!animation.has("frames")) {
				continue;
			}
			for (var element : animation.getAsJsonArray("frames")) {
				int index = element.isJsonObject()
						? element.getAsJsonObject().get("index").getAsInt()
						: element.getAsInt();

				if (index < 0 || index >= frames) {
					wrong.add(image.getFileName() + " names frame " + index + " of " + frames);
				}
			}
		}

		assertTrue(wrong.isEmpty(), String.join("; ", wrong));
	}
}
