package dev.fimkov.echoesbeneath.client.input;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import com.mojang.blaze3d.platform.InputConstants;
import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.client.screen.EchoConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class EchoKeys {
	public static final KeyMapping.Category ECHO_CATEGORY =
			KeyMapping.Category.register(EchoesBeneath.id("main"));

	public static final KeyMapping OPEN_CONFIG = new KeyMapping(EchoTranslations.CONFIG_OPEN_KEY,
			InputConstants.Type.KEYSYM, InputConstants.KEY_K, ECHO_CATEGORY);

	private EchoKeys() {
	}

	public static void poll() {
		Minecraft client = Minecraft.getInstance();
		boolean pressed = false;

		while (OPEN_CONFIG.consumeClick()) {
			pressed = true;
		}

		if (pressed && client.screen == null) {
			client.setScreenAndShow(new EchoConfigScreen(null));
		}
	}
}
