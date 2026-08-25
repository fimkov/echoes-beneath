package dev.fimkov.echoesbeneath.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public final class EchoDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(EchoRecipeProvider::new);
		pack.addProvider(EchoEntityTypeTagProvider::new);
		pack.addProvider(EchoModelProvider::new);
		pack.addProvider(EchoLootTableProvider::new);
		pack.addProvider(EchoAdvancementProvider::new);
	}
}
