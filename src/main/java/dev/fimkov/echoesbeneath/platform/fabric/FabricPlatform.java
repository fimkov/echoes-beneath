package dev.fimkov.echoesbeneath.platform.fabric;

import dev.fimkov.echoesbeneath.platform.EchoNetwork;
import dev.fimkov.echoesbeneath.platform.EchoPlatform;
import dev.fimkov.echoesbeneath.platform.EchoRegistries;
import dev.fimkov.echoesbeneath.platform.EchoServerHooks;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class FabricPlatform implements EchoPlatform {
	private final EchoNetwork network = new FabricNetwork();
	private final EchoServerHooks server = new FabricServerHooks();
	private final EchoRegistries registries = new FabricRegistries();

	@Override
	public Path configDirectory() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public String name() {
		return "fabric";
	}

	@Override
	public EchoNetwork network() {
		return network;
	}

	@Override
	public EchoServerHooks server() {
		return server;
	}

	@Override
	public EchoRegistries registries() {
		return registries;
	}
}
