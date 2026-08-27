package dev.fimkov.echoesbeneath.datagen;

import dev.fimkov.echoesbeneath.worldgen.EchoStructures;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class EchoLootTableProvider extends SimpleFabricLootTableSubProvider {
	public EchoLootTableProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
		super(output, lookup, LootContextParamSets.CHEST);
	}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> exporter) {
		exporter.accept(EchoStructures.LISTENING_POST_LOOT, listeningPost());
		exporter.accept(EchoStructures.RESONANCE_SHRINE_LOOT, resonanceShrine());
		exporter.accept(EchoStructures.SURFACE_RUIN_LOOT, surfaceRuin());
		exporter.accept(EchoStructures.RESONANCE_VAULT_LOOT, resonanceVault());
	}

	private LootTable.Builder listeningPost() {
		return LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(UniformGenerator.between(2.0F, 4.0F))
						.add(LootItem.lootTableItem(Items.AMETHYST_SHARD)
								.setWeight(24)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
						.add(LootItem.lootTableItem(Items.CANDLE)
								.setWeight(18)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
						.add(LootItem.lootTableItem(Items.WOOL.cyan())
								.setWeight(14)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
						.add(LootItem.lootTableItem(Items.COAL)
								.setWeight(14)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
						.add(LootItem.lootTableItem(Items.BOOK).setWeight(10))
						.add(LootItem.lootTableItem(Items.GLOW_BERRIES)
								.setWeight(8)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
						.add(LootItem.lootTableItem(Items.COMPASS).setWeight(4))
						.add(LootItem.lootTableItem(Blocks.SCULK_VEIN.asItem())
								.setWeight(6)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
						.add(LootItem.lootTableItem(Items.EXPERIENCE_BOTTLE)
								.setWeight(2)
								.apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))));
	}

	private LootTable.Builder resonanceShrine() {
		return LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(UniformGenerator.between(2.0F, 3.0F))
						.add(LootItem.lootTableItem(Items.AMETHYST_SHARD)
								.setWeight(26)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
						.add(LootItem.lootTableItem(Blocks.SCULK.asItem())
								.setWeight(16)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
						.add(LootItem.lootTableItem(Items.CANDLE)
								.setWeight(14)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
						.add(LootItem.lootTableItem(Items.BOOK).setWeight(12))
						.add(LootItem.lootTableItem(Items.WOOL.lightGray())
								.setWeight(10)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
						.add(LootItem.lootTableItem(Items.EXPERIENCE_BOTTLE)
								.setWeight(4)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
						.add(LootItem.lootTableItem(Items.ECHO_SHARD).setWeight(2))
						.add(LootItem.lootTableItem(Items.RECOVERY_COMPASS).setWeight(1)));
	}

	private LootTable.Builder resonanceVault() {
		return LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(UniformGenerator.between(3.0F, 5.0F))
						.add(LootItem.lootTableItem(Items.AMETHYST_SHARD)
								.setWeight(30)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 7.0F))))
						.add(LootItem.lootTableItem(Items.EXPERIENCE_BOTTLE)
								.setWeight(20)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
						.add(LootItem.lootTableItem(Items.CANDLE)
								.setWeight(12)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
						.add(LootItem.lootTableItem(Items.BOOK).setWeight(10))
						.add(LootItem.lootTableItem(Blocks.SCULK.asItem())
								.setWeight(10)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
						.add(LootItem.lootTableItem(Items.ECHO_SHARD).setWeight(3))
						.add(LootItem.lootTableItem(Items.RECOVERY_COMPASS).setWeight(1)));
	}

	private LootTable.Builder surfaceRuin() {
		return LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(UniformGenerator.between(1.0F, 3.0F))
						.add(LootItem.lootTableItem(Items.COMPASS).setWeight(20))
						.add(LootItem.lootTableItem(Items.MAP).setWeight(16))
						.add(LootItem.lootTableItem(Items.CANDLE)
								.setWeight(16)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
						.add(LootItem.lootTableItem(Items.BOOK).setWeight(14))
						.add(LootItem.lootTableItem(Items.COAL)
								.setWeight(14)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
						.add(LootItem.lootTableItem(Items.AMETHYST_SHARD)
								.setWeight(12)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
						.add(LootItem.lootTableItem(Items.MOSS_BLOCK)
								.setWeight(8)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
						.add(LootItem.lootTableItem(Items.EXPERIENCE_BOTTLE).setWeight(3))
						.add(LootItem.lootTableItem(Items.ECHO_SHARD).setWeight(1)));
	}
}
