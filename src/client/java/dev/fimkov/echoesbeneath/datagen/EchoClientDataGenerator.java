package dev.fimkov.echoesbeneath.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

// One entry point for the whole mod: the model provider is client only, and a
// second pack would sweep away what the first one wrote.
public final class EchoClientDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(EchoRecipeProvider::new);
		pack.addProvider(EchoEntityTypeTagProvider::new);
		pack.addProvider(EchoLootTableProvider::new);
		pack.addProvider(EchoAdvancementProvider::new);
		pack.addProvider(EchoModelProvider::new);
	}
}
