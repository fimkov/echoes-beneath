package dev.fimkov.echoesbeneath.registry;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.block.ResonanceTableBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class EchoBlocks {
	private static final float HARDNESS = 3.5F;
	private static final float BLAST_RESISTANCE = 6.0F;
	private static final int GLOW_LEVEL = 5;

	public static final Block RESONANCE_TABLE = new ResonanceTableBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILES)
					.strength(HARDNESS, BLAST_RESISTANCE)
					.sound(SoundType.DEEPSLATE_TILES)
					.lightLevel(state -> GLOW_LEVEL)
					.noOcclusion());

	public static final BlockItem RESONANCE_TABLE_ITEM = new BlockItem(RESONANCE_TABLE,
			new Item.Properties().rarity(Rarity.RARE));

	private EchoBlocks() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.BLOCK, EchoesBeneath.id("resonance_table"), RESONANCE_TABLE);
		Registry.register(BuiltInRegistries.ITEM, EchoesBeneath.id("resonance_table"), RESONANCE_TABLE_ITEM);
	}
}
