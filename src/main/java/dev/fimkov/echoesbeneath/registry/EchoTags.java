package dev.fimkov.echoesbeneath.registry;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class EchoTags {
	public static final TagKey<EntityType<?>> RECORDING_BLACKLIST =
			TagKey.create(Registries.ENTITY_TYPE, EchoesBeneath.id("recording_blacklist"));

	public static final TagKey<EntityType<?>> TEMPORARILY_SUPPRESSED =
			TagKey.create(Registries.ENTITY_TYPE, EchoesBeneath.id("temporarily_suppressed"));

	private EchoTags() {
	}

	public static void touch() {
	}
}
