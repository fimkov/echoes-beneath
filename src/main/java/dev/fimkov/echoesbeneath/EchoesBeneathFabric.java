package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import dev.fimkov.echoesbeneath.platform.fabric.FabricPlatform;
import net.fabricmc.api.ModInitializer;

public final class EchoesBeneathFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		EchoPlatforms.install(new FabricPlatform());
		EchoesBeneathBootstrap.bootstrap();
	}
}
