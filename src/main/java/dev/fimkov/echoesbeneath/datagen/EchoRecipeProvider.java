package dev.fimkov.echoesbeneath.datagen;

import net.minecraft.world.item.Item;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderGetter;
import dev.fimkov.echoesbeneath.registry.EchoBlocks;
import dev.fimkov.echoesbeneath.registry.EchoItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
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
	private static final char BLOCK_KEY = 'B';
	private static final char MAP_KEY = 'M';
	private static final char DEEPSLATE_KEY = 'D';

	public EchoRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
		super(output, lookup);
	}

	@Override
	protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput exporter) {
		return new RecipeProvider(registries, exporter) {
			@Override
			public void buildRecipes() {
				HolderGetter<Item> items = registries.lookupOrThrow(Registries.ITEM);

			ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, EchoItems.ECHO_LENS)
					.pattern("EEE")
					.pattern("EAE")
					.pattern("EEE")
					.define(ECHO_SHARD_KEY, Items.ECHO_SHARD)
					.define(AMETHYST_KEY, Items.AMETHYST_SHARD)
					.unlockedBy(RecipeProvider.getHasName(Items.ECHO_SHARD), this.has(Items.ECHO_SHARD))
					.save(this.output);

			ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, EchoBlocks.RESONANCE_TABLE_ITEM)
					.pattern(" E ")
					.pattern("ABA")
					.pattern("DDD")
					.define(ECHO_SHARD_KEY, Items.ECHO_SHARD)
					.define(AMETHYST_KEY, Items.AMETHYST_SHARD)
					.define(BLOCK_KEY, Items.AMETHYST_BLOCK)
					.define(DEEPSLATE_KEY, Items.DEEPSLATE_TILES)
					.unlockedBy(RecipeProvider.getHasName(Items.ECHO_SHARD), this.has(Items.ECHO_SHARD))
					.save(this.output);

			ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, EchoBlocks.SOUNDING_TABLE_ITEM)
					.pattern(" M ")
					.pattern("EBE")
					.pattern("DDD")
					.define(ECHO_SHARD_KEY, Items.ECHO_SHARD)
					.define(BLOCK_KEY, Items.AMETHYST_BLOCK)
					.define(MAP_KEY, Items.MAP)
					.define(DEEPSLATE_KEY, Items.DEEPSLATE_TILES)
					.unlockedBy(RecipeProvider.getHasName(Items.ECHO_SHARD), this.has(Items.ECHO_SHARD))
					.save(this.output);
				}
		};
	}

	@Override
	public String getName() {
		return "Echoes Beneath recipes";
	}
}
