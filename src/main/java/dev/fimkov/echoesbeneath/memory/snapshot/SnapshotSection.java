package dev.fimkov.echoesbeneath.memory.snapshot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.SimpleBitStorage;

import java.util.Arrays;

public final class SnapshotSection {
	public static final int SECTION_SIZE = 16;
	public static final int CELLS_PER_SECTION = SECTION_SIZE * SECTION_SIZE * SECTION_SIZE;
	private static final int MINIMUM_BITS = 1;

	private final int[] localPalette;
	private final SimpleBitStorage cells;

	private SnapshotSection(int[] localPalette, SimpleBitStorage cells) {
		this.localPalette = localPalette;
		this.cells = cells;
	}

	public static SnapshotSection compact(int[] globalIndices) {
		int[] distinct = distinctSorted(globalIndices);
		int bits = Math.max(MINIMUM_BITS, requiredBits(distinct.length));
		SimpleBitStorage storage = new SimpleBitStorage(bits, CELLS_PER_SECTION);

		for (int cell = 0; cell < CELLS_PER_SECTION; cell++) {
			storage.set(cell, Arrays.binarySearch(distinct, globalIndices[cell]));
		}

		return new SnapshotSection(distinct, storage);
	}

	private static int[] distinctSorted(int[] values) {
		int[] copy = values.clone();
		Arrays.sort(copy);
		int unique = 0;

		for (int index = 0; index < copy.length; index++) {
			if (index == 0 || copy[index] != copy[index - 1]) {
				copy[unique++] = copy[index];
			}
		}

		return Arrays.copyOf(copy, unique);
	}

	private static int requiredBits(int paletteSize) {
		return Math.max(MINIMUM_BITS, 32 - Integer.numberOfLeadingZeros(Math.max(1, paletteSize - 1)));
	}

	public static int cellIndex(int localX, int localY, int localZ) {
		return (localY << 8) | (localZ << 4) | localX;
	}

	public int globalIndexAt(int localX, int localY, int localZ) {
		int local = cells.get(cellIndex(localX, localY, localZ));

		if (local < 0 || local >= localPalette.length) {
			return BlockPalette.AIR_INDEX;
		}

		return localPalette[local];
	}

	public boolean isUniformAir() {
		return localPalette.length == 1 && localPalette[0] == BlockPalette.AIR_INDEX;
	}

	public CompoundTag toNbt() {
		CompoundTag tag = new CompoundTag();
		tag.putIntArray("palette", localPalette);
		tag.putInt("bits", cells.getBits());
		tag.putLongArray("cells", cells.getRaw());

		return tag;
	}

	public static SnapshotSection fromNbt(CompoundTag tag) {
		int[] palette = tag.getIntArray("palette").orElse(new int[0]);
		int bits = tag.getIntOr("bits", 0);
		long[] raw = tag.getLongArray("cells").orElse(new long[0]);

		if (palette.length == 0 || bits <= 0) {
			throw new IllegalArgumentException("Snapshot section has no palette");
		}

		return new SnapshotSection(palette, new SimpleBitStorage(bits, CELLS_PER_SECTION, raw));
	}
}
