package dev.fimkov.echoesbeneath.datagen;

import dev.fimkov.echoesbeneath.registry.EchoBlocks;
import dev.fimkov.echoesbeneath.registry.EchoItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class EchoRecipeProvider extends FabricRecipeProvider {
	private static final char ECHO_SHARD_KEY = 'E';
	private static final char AMETHYST_KEY = 'A';
	private static final char TABLE_KEY = 'T';
	private static final char DEEPSLATE_KEY = 'D';

	public EchoRecipeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
		super(output, lookup);
	}

	@Override
	public void buildRecipes(RecipeOutput exporter) {
		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, EchoItems.ECHO_LENS)
				.pattern("EEE")
				.pattern("EAE")
				.pattern("EEE")
				.define(ECHO_SHARD_KEY, Items.ECHO_SHARD)
				.define(AMETHYST_KEY, Items.AMETHYST_SHARD)
				.unlockedBy(getHasName(Items.ECHO_SHARD), has(Items.ECHO_SHARD))
				.save(exporter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, EchoBlocks.RESONANCE_TABLE_ITEM)
				.pattern(" E ")
				.pattern("ATA")
				.pattern("DDD")
				.define(ECHO_SHARD_KEY, Items.ECHO_SHARD)
				.define(AMETHYST_KEY, Items.AMETHYST_SHARD)
				.define(TABLE_KEY, Items.ENCHANTING_TABLE)
				.define(DEEPSLATE_KEY, Items.DEEPSLATE_TILES)
				.unlockedBy(getHasName(Items.ECHO_SHARD), has(Items.ECHO_SHARD))
				.save(exporter);
	}
}
