package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import dev.fimkov.echoesbeneath.platform.fabric.FabricPlatform;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EchoesBeneath implements ModInitializer {
	public static final String MOD_ID = "echoes_beneath";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		EchoPlatforms.install(new FabricPlatform());
		EchoesBeneathBootstrap.bootstrap();
	}
}
