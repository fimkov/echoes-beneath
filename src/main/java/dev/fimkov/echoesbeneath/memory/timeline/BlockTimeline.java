package dev.fimkov.echoesbeneath.memory.timeline;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.nbt.CompoundTag;

public final class BlockTimeline {
	public static final int STRIDE = 4;
	private static final int OFFSET_TICK = 0;
	private static final int OFFSET_POSITION = 1;
	private static final int OFFSET_PREVIOUS_STATE = 2;
	private static final int OFFSET_NEW_STATE = 3;

	private final IntList data;
	private final int maxEvents;

	public BlockTimeline(int maxEvents) {
		this.data = new IntArrayList();
		this.maxEvents = maxEvents;
	}

	private BlockTimeline(IntList data, int maxEvents) {
		this.data = data;
		this.maxEvents = maxEvents;
	}

	public boolean record(int tick, int packedPosition, int previousStateIndex, int newStateIndex) {
		if (size() >= maxEvents) {
			return false;
		}
		data.add(tick);
		data.add(packedPosition);
		data.add(previousStateIndex);
		data.add(newStateIndex);
		return true;
	}

	public int size() {
		return data.size() / STRIDE;
	}

	public int tickAt(int index) {
		return data.getInt(index * STRIDE + OFFSET_TICK);
	}

	public int packedPositionAt(int index) {
		return data.getInt(index * STRIDE + OFFSET_POSITION);
	}

	public int previousStateAt(int index) {
		return data.getInt(index * STRIDE + OFFSET_PREVIOUS_STATE);
	}

	public int newStateAt(int index) {
		return data.getInt(index * STRIDE + OFFSET_NEW_STATE);
	}

	public int[] rawData() {
		return data.toIntArray();
	}

	public CompoundTag toNbt() {
		CompoundTag tag = new CompoundTag();
		tag.putIntArray("events", data.toIntArray());
		return tag;
	}

	public static BlockTimeline fromNbt(CompoundTag tag, int maxEvents) {
		int[] raw = tag.getIntArray("events");
		if (raw.length % STRIDE != 0) {
			throw new IllegalArgumentException("Block timeline length " + raw.length + " is not a multiple of " + STRIDE);
		}
		return new BlockTimeline(new IntArrayList(raw), maxEvents);
	}

	public static BlockTimeline fromRawData(int[] raw, int maxEvents) {
		if (raw.length % STRIDE != 0) {
			throw new IllegalArgumentException("Block timeline length " + raw.length + " is not a multiple of " + STRIDE);
		}
		return new BlockTimeline(new IntArrayList(raw), maxEvents);
	}
}
