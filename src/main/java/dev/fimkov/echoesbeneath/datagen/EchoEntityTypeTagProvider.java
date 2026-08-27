package dev.fimkov.echoesbeneath.datagen;

import net.minecraft.world.entity.EntityTypes;
import dev.fimkov.echoesbeneath.registry.EchoEntities;
import dev.fimkov.echoesbeneath.registry.EchoTags;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;

import java.util.concurrent.CompletableFuture;

public class EchoEntityTypeTagProvider extends FabricTagsProvider<EntityType<?>> {
	public EchoEntityTypeTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
		super(output, Registries.ENTITY_TYPE, lookup);
	}

	@Override
	protected void addTags(HolderLookup.Provider lookup) {
		getOrCreateTagBuilder(EchoTags.RECORDING_BLACKLIST)
				.add(EchoEntities.ECHO_LENS)
				.add(EntityTypes.MARKER)
				.add(EntityTypes.ITEM_DISPLAY)
				.add(EntityTypes.BLOCK_DISPLAY)
				.add(EntityTypes.TEXT_DISPLAY)
				.add(EntityTypes.INTERACTION)
				.add(EntityTypes.AREA_EFFECT_CLOUD)
				.add(EntityTypes.EXPERIENCE_ORB)
				.add(EntityTypes.LIGHTNING_BOLT);

		getOrCreateTagBuilder(EchoTags.TEMPORARILY_SUPPRESSED)
				.addOptionalTag(EntityTypeTags.ZOMBIES)
				.addOptionalTag(EntityTypeTags.SKELETONS)
				.addOptionalTag(EntityTypeTags.RAIDERS)
				.addOptionalTag(EntityTypeTags.ILLAGER)
				.add(EntityTypes.CREEPER)
				.add(EntityTypes.SPIDER)
				.add(EntityTypes.CAVE_SPIDER)
				.add(EntityTypes.ENDERMAN)
				.add(EntityTypes.ENDERMITE)
				.add(EntityTypes.SILVERFISH)
				.add(EntityTypes.WITCH)
				.add(EntityTypes.SLIME)
				.add(EntityTypes.MAGMA_CUBE)
				.add(EntityTypes.BLAZE)
				.add(EntityTypes.GHAST)
				.add(EntityTypes.PHANTOM)
				.add(EntityTypes.GUARDIAN)
				.add(EntityTypes.ELDER_GUARDIAN)
				.add(EntityTypes.SHULKER)
				.add(EntityTypes.WARDEN)
				.add(EntityTypes.PIGLIN_BRUTE)
				.add(EntityTypes.HOGLIN)
				.add(EntityTypes.ZOGLIN)
				.add(EntityTypes.VEX)
				.add(EntityTypes.BREEZE)
				.add(EntityTypes.BOGGED)
				.add(EntityTypes.ZOMBIFIED_PIGLIN);
	}
}
