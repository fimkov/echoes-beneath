package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.config.EchoConfig;
import dev.fimkov.echoesbeneath.registry.EchoComponents;
import dev.fimkov.echoesbeneath.registry.EchoItems;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import dev.fimkov.echoesbeneath.registry.EchoTags;

public final class EchoesBeneathBootstrap {
	private EchoesBeneathBootstrap() {
	}

	public static void bootstrap() {
		EchoConfig.load();
		EchoComponents.register();
		EchoItems.register();
		EchoSounds.register();
		EchoTags.touch();
	}
}
