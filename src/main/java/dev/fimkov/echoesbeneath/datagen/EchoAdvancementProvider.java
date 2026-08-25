package dev.fimkov.echoesbeneath.datagen;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class EchoAdvancementProvider extends FabricAdvancementProvider {
	private static final String TITLE_KEY = "advancements.echoes_beneath.echo_shard.title";
	private static final String DESCRIPTION_KEY = "advancements.echoes_beneath.echo_shard.description";
	private static final String CRITERION_NAME = "has_echo_shard";

	public EchoAdvancementProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
		super(output, lookup);
	}

	@Override
	public void generateAdvancement(HolderLookup.Provider lookup, Consumer<AdvancementHolder> consumer) {
		Advancement.Builder.advancement()
				.display(Items.ECHO_SHARD,
						Component.translatable(TITLE_KEY),
						Component.translatable(DESCRIPTION_KEY),
						null,
						AdvancementType.TASK,
						true,
						true,
						false)
				.addCriterion(CRITERION_NAME, InventoryChangeTrigger.TriggerInstance.hasItems(Items.ECHO_SHARD))
				.save(consumer, EchoesBeneath.id("echo_shard").toString());
	}
}
