package dev.fimkov.echoesbeneath.datagen;

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
	}
}
