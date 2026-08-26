package dev.fimkov.echoesbeneath.platform.neoforge;

import dev.fimkov.echoesbeneath.platform.EchoNetwork;
import dev.fimkov.echoesbeneath.platform.EchoPlatform;
import dev.fimkov.echoesbeneath.platform.EchoRegistries;
import dev.fimkov.echoesbeneath.platform.EchoServerHooks;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class NeoForgePlatform implements EchoPlatform {
	private final NeoForgeNetwork network = new NeoForgeNetwork();
	private final NeoForgeServerHooks server = new NeoForgeServerHooks();
	private final NeoForgeRegistries registries = new NeoForgeRegistries();

	@Override
	public Path configDirectory() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public String name() {
		return "neoforge";
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

	public NeoForgeNetwork neoNetwork() {
		return network;
	}

	public NeoForgeRegistries neoRegistries() {
		return registries;
	}
}
