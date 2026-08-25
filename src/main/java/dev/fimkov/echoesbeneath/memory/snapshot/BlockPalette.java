package dev.fimkov.echoesbeneath.memory.snapshot;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class BlockPalette {
	public static final int AIR_INDEX = 0;
	private static final int MAX_ENTRIES = 65535;

	private final List<BlockState> states = new ArrayList<>();
	private final Object2IntMap<BlockState> indices = new Object2IntOpenHashMap<>();

	public BlockPalette() {
		indices.defaultReturnValue(-1);
		add(Blocks.AIR.defaultBlockState());
	}

	private int add(BlockState state) {
		int index = states.size();
		states.add(state);
		indices.put(state, index);
		return index;
	}

	public int indexOf(BlockState state) {
		int existing = indices.getInt(state);
		if (existing >= 0) {
			return existing;
		}
		if (states.size() >= MAX_ENTRIES) {
			return AIR_INDEX;
		}
		return add(state);
	}

	public BlockState stateAt(int index) {
		if (index < 0 || index >= states.size()) {
			return Blocks.AIR.defaultBlockState();
		}
		return states.get(index);
	}

	public int size() {
		return states.size();
	}

	public List<BlockState> states() {
		return List.copyOf(states);
	}

	public ListTag toNbt() {
		ListTag list = new ListTag();
		for (BlockState state : states) {
			list.add(NbtUtils.writeBlockState(state));
		}
		return list;
	}

	public static BlockPalette fromNbt(ListTag list, HolderGetter<Block> blockLookup) {
		BlockPalette palette = new BlockPalette();
		palette.states.clear();
		palette.indices.clear();
		for (Tag element : list) {
			BlockState state = NbtUtils.readBlockState(blockLookup, (CompoundTag) element);
			palette.add(state);
		}
		if (palette.states.isEmpty()) {
			palette.add(Blocks.AIR.defaultBlockState());
		}
		return palette;
	}
}
