package dev.fimkov.echoesbeneath.datagen;

import dev.fimkov.echoesbeneath.registry.EchoItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.model.ModelTemplates;

public class EchoModelProvider extends FabricModelProvider {
	public EchoModelProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators generators) {
	}

	@Override
	public void generateItemModels(ItemModelGenerators generators) {
		generators.generateFlatItem(EchoItems.ECHO_LENS, ModelTemplates.FLAT_ITEM);
	}
}
