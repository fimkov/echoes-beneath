package dev.fimkov.echoesbeneath.worldgen;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;

public final class EchoStructures {
	public static final String ECHO_LISTENING_POST = "echo_listening_post";
	public static final String COLLAPSED_RESONANCE_SHRINE = "collapsed_resonance_shrine";
	public static final String SURFACE_ECHO_RUIN = "surface_echo_ruin";

	public static final List<String> ALL = List.of(
			ECHO_LISTENING_POST, COLLAPSED_RESONANCE_SHRINE, SURFACE_ECHO_RUIN);

	public static final ResourceKey<LootTable> LISTENING_POST_LOOT = lootTable(ECHO_LISTENING_POST);
	public static final ResourceKey<LootTable> RESONANCE_SHRINE_LOOT = lootTable(COLLAPSED_RESONANCE_SHRINE);
	public static final ResourceKey<LootTable> SURFACE_RUIN_LOOT = lootTable(SURFACE_ECHO_RUIN);

	private EchoStructures() {
	}

	public static ResourceKey<Structure> structureKey(String name) {
		return ResourceKey.create(Registries.STRUCTURE, EchoesBeneath.id(name));
	}

	private static ResourceKey<LootTable> lootTable(String name) {
		return ResourceKey.create(Registries.LOOT_TABLE, EchoesBeneath.id("chests/" + name));
	}
}
