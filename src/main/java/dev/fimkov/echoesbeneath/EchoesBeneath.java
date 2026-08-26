package dev.fimkov.echoesbeneath;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EchoesBeneath {
	public static final String MOD_ID = "echoes_beneath";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private EchoesBeneath() {
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
