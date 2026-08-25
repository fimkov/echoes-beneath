package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.command.EchoCommands;
import dev.fimkov.echoesbeneath.config.EchoConfig;
import dev.fimkov.echoesbeneath.network.EchoNetworkPayloads;
import dev.fimkov.echoesbeneath.registry.EchoComponents;
import dev.fimkov.echoesbeneath.registry.EchoEntities;
import dev.fimkov.echoesbeneath.registry.EchoItemGroups;
import dev.fimkov.echoesbeneath.registry.EchoItems;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import dev.fimkov.echoesbeneath.registry.EchoTags;
import dev.fimkov.echoesbeneath.runtime.EchoServerLifecycle;

public final class EchoesBeneathBootstrap {
	private EchoesBeneathBootstrap() {
	}

	public static void bootstrap() {
		EchoConfig.load();
		EchoComponents.register();
		EchoItems.register();
		EchoItemGroups.register();
		EchoSounds.register();
		EchoEntities.register();
		EchoTags.touch();
		EchoNetworkPayloads.registerCommon();
		EchoCommands.register();
		EchoServerLifecycle.register();
	}
}
