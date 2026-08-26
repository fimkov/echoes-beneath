package dev.fimkov.echoesbeneath.registry;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.block.EchoChartBlock;
import dev.fimkov.echoesbeneath.block.ResonanceTableBlock;
import dev.fimkov.echoesbeneath.block.SoundingTableBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

public final class EchoBlocks {
	private static final float HARDNESS = 3.5F;
	private static final float BLAST_RESISTANCE = 6.0F;
	private static final int GLOW_LEVEL = 5;
	private static final float CHART_HARDNESS = 0.6F;
	private static final int CHART_GLOW = 7;

	public static final Block RESONANCE_TABLE = new ResonanceTableBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILES)
					.strength(HARDNESS, BLAST_RESISTANCE)
					.sound(SoundType.DEEPSLATE_TILES)
					.lightLevel(state -> GLOW_LEVEL)
					.noOcclusion());

	public static final Block SOUNDING_TABLE = new SoundingTableBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILES)
					.strength(HARDNESS, BLAST_RESISTANCE)
					.sound(SoundType.DEEPSLATE_TILES)
					.lightLevel(state -> GLOW_LEVEL)
					.noOcclusion());

	public static final BlockItem SOUNDING_TABLE_ITEM = new BlockItem(SOUNDING_TABLE,
			new Item.Properties().rarity(Rarity.RARE));

	public static final BlockItem RESONANCE_TABLE_ITEM = new BlockItem(RESONANCE_TABLE,
			new Item.Properties().rarity(Rarity.RARE));

	public static final Block ECHO_CHART = new EchoChartBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK)
					.strength(CHART_HARDNESS, CHART_HARDNESS)
					.sound(SoundType.AMETHYST)
					.lightLevel(state -> CHART_GLOW)
					.noOcclusion()
					.noCollission()
					.pushReaction(PushReaction.DESTROY));

	private EchoBlocks() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.BLOCK, EchoesBeneath.id("resonance_table"), RESONANCE_TABLE);
		Registry.register(BuiltInRegistries.ITEM, EchoesBeneath.id("resonance_table"), RESONANCE_TABLE_ITEM);
		Registry.register(BuiltInRegistries.BLOCK, EchoesBeneath.id("sounding_table"), SOUNDING_TABLE);
		Registry.register(BuiltInRegistries.ITEM, EchoesBeneath.id("sounding_table"), SOUNDING_TABLE_ITEM);
		Registry.register(BuiltInRegistries.BLOCK, EchoesBeneath.id("echo_chart"), ECHO_CHART);
	}
}
