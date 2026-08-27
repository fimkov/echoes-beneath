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
		builder(EchoTags.RECORDING_BLACKLIST)
				.add(EchoEntities.ECHO_LENS.builtInRegistryHolder().key())
				.add(EntityTypes.MARKER.builtInRegistryHolder().key())
				.add(EntityTypes.ITEM_DISPLAY.builtInRegistryHolder().key())
				.add(EntityTypes.BLOCK_DISPLAY.builtInRegistryHolder().key())
				.add(EntityTypes.TEXT_DISPLAY.builtInRegistryHolder().key())
				.add(EntityTypes.INTERACTION.builtInRegistryHolder().key())
				.add(EntityTypes.AREA_EFFECT_CLOUD.builtInRegistryHolder().key())
				.add(EntityTypes.EXPERIENCE_ORB.builtInRegistryHolder().key())
				.add(EntityTypes.LIGHTNING_BOLT.builtInRegistryHolder().key());

		builder(EchoTags.TEMPORARILY_SUPPRESSED)
				.addOptionalTag(EntityTypeTags.ZOMBIES)
				.addOptionalTag(EntityTypeTags.SKELETONS)
				.addOptionalTag(EntityTypeTags.RAIDERS)
				.addOptionalTag(EntityTypeTags.ILLAGER)
				.add(EntityTypes.CREEPER.builtInRegistryHolder().key())
				.add(EntityTypes.SPIDER.builtInRegistryHolder().key())
				.add(EntityTypes.CAVE_SPIDER.builtInRegistryHolder().key())
				.add(EntityTypes.ENDERMAN.builtInRegistryHolder().key())
				.add(EntityTypes.ENDERMITE.builtInRegistryHolder().key())
				.add(EntityTypes.SILVERFISH.builtInRegistryHolder().key())
				.add(EntityTypes.WITCH.builtInRegistryHolder().key())
				.add(EntityTypes.SLIME.builtInRegistryHolder().key())
				.add(EntityTypes.MAGMA_CUBE.builtInRegistryHolder().key())
				.add(EntityTypes.BLAZE.builtInRegistryHolder().key())
				.add(EntityTypes.GHAST.builtInRegistryHolder().key())
				.add(EntityTypes.PHANTOM.builtInRegistryHolder().key())
				.add(EntityTypes.GUARDIAN.builtInRegistryHolder().key())
				.add(EntityTypes.ELDER_GUARDIAN.builtInRegistryHolder().key())
				.add(EntityTypes.SHULKER.builtInRegistryHolder().key())
				.add(EntityTypes.WARDEN.builtInRegistryHolder().key())
				.add(EntityTypes.PIGLIN_BRUTE.builtInRegistryHolder().key())
				.add(EntityTypes.HOGLIN.builtInRegistryHolder().key())
				.add(EntityTypes.ZOGLIN.builtInRegistryHolder().key())
				.add(EntityTypes.VEX.builtInRegistryHolder().key())
				.add(EntityTypes.BREEZE.builtInRegistryHolder().key())
				.add(EntityTypes.BOGGED.builtInRegistryHolder().key())
				.add(EntityTypes.ZOMBIFIED_PIGLIN.builtInRegistryHolder().key());
	}
}
