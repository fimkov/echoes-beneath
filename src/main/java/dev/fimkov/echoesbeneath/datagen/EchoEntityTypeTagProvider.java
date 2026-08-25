package dev.fimkov.echoesbeneath.datagen;

import dev.fimkov.echoesbeneath.registry.EchoEntities;
import dev.fimkov.echoesbeneath.registry.EchoTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;

import java.util.concurrent.CompletableFuture;

public class EchoEntityTypeTagProvider extends FabricTagProvider<EntityType<?>> {
	public EchoEntityTypeTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
		super(output, Registries.ENTITY_TYPE, lookup);
	}

	@Override
	protected void addTags(HolderLookup.Provider lookup) {
		getOrCreateTagBuilder(EchoTags.RECORDING_BLACKLIST)
				.add(EchoEntities.ECHO_LENS)
				.add(EntityType.MARKER)
				.add(EntityType.ITEM_DISPLAY)
				.add(EntityType.BLOCK_DISPLAY)
				.add(EntityType.TEXT_DISPLAY)
				.add(EntityType.INTERACTION)
				.add(EntityType.AREA_EFFECT_CLOUD)
				.add(EntityType.EXPERIENCE_ORB)
				.add(EntityType.LIGHTNING_BOLT);

		getOrCreateTagBuilder(EchoTags.TEMPORARILY_SUPPRESSED)
				.addOptionalTag(EntityTypeTags.ZOMBIES)
				.addOptionalTag(EntityTypeTags.SKELETONS)
				.addOptionalTag(EntityTypeTags.RAIDERS)
				.addOptionalTag(EntityTypeTags.ILLAGER)
				.add(EntityType.CREEPER)
				.add(EntityType.SPIDER)
				.add(EntityType.CAVE_SPIDER)
				.add(EntityType.ENDERMAN)
				.add(EntityType.ENDERMITE)
				.add(EntityType.SILVERFISH)
				.add(EntityType.WITCH)
				.add(EntityType.SLIME)
				.add(EntityType.MAGMA_CUBE)
				.add(EntityType.BLAZE)
				.add(EntityType.GHAST)
				.add(EntityType.PHANTOM)
				.add(EntityType.GUARDIAN)
				.add(EntityType.ELDER_GUARDIAN)
				.add(EntityType.SHULKER)
				.add(EntityType.WARDEN)
				.add(EntityType.PIGLIN_BRUTE)
				.add(EntityType.HOGLIN)
				.add(EntityType.ZOGLIN)
				.add(EntityType.VEX)
				.add(EntityType.BREEZE)
				.add(EntityType.BOGGED)
				.add(EntityType.ZOMBIFIED_PIGLIN);
	}
}
