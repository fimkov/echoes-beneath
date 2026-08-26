package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.advancement.EchoCriteria;
import dev.fimkov.echoesbeneath.command.EchoCommands;
import dev.fimkov.echoesbeneath.config.EchoConfig;
import dev.fimkov.echoesbeneath.network.EchoNetworkPayloads;
import dev.fimkov.echoesbeneath.registry.EchoBlockEntities;
import dev.fimkov.echoesbeneath.registry.EchoBlocks;
import dev.fimkov.echoesbeneath.registry.EchoComponents;
import dev.fimkov.echoesbeneath.registry.EchoEntities;
import dev.fimkov.echoesbeneath.registry.EchoItemGroups;
import dev.fimkov.echoesbeneath.registry.EchoItems;
import dev.fimkov.echoesbeneath.registry.EchoMenus;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import dev.fimkov.echoesbeneath.registry.EchoTags;
import dev.fimkov.echoesbeneath.runtime.EchoServerLifecycle;

public final class EchoesBeneathBootstrap {
	private EchoesBeneathBootstrap() {
	}

	public static void registerContent() {
		EchoCriteria.register();
		EchoComponents.register();
		EchoBlocks.register();
		EchoBlockEntities.register();
		EchoItems.register();
		EchoMenus.register();
		EchoItemGroups.register();
		EchoSounds.register();
		EchoEntities.register();
		EchoTags.touch();
	}

	public static void registerBehaviour() {
		EchoConfig.load();
		EchoNetworkPayloads.registerCommon();
		EchoCommands.register();
		EchoServerLifecycle.register();
	}

	public static void bootstrap() {
		registerContent();
		registerBehaviour();
	}
}
