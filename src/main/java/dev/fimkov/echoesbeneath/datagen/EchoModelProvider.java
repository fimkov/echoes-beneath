package dev.fimkov.echoesbeneath.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;

public class EchoModelProvider extends FabricModelProvider {
	public EchoModelProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators generators) {
	}

	@Override
	public void generateItemModels(ItemModelGenerators generators) {
	}
}
