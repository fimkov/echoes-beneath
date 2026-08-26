package dev.fimkov.echoesbeneath.client.chart;

import dev.fimkov.echoesbeneath.chart.EchoChartCloud;
import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class EchoChartModel {
	public static final int GRID = 48;
	private static final int GRID_BITS = 6;
	private static final int GRID_MASK = (1 << GRID_BITS) - 1;
	private static final int CHANNEL_BITS = 20;
	private static final long CHANNEL_MASK = (1L << CHANNEL_BITS) - 1L;
	private static final int BYTE_MASK = 0xFF;

	private final IntList cells = new IntArrayList();
	private final IntList colours = new IntArrayList();
	private final IntSet occupied = new IntOpenHashSet();
	private final int spanX;
	private final int spanY;
	private final int spanZ;

	private EchoChartModel(int spanX, int spanY, int spanZ) {
		this.spanX = spanX;
		this.spanY = spanY;
		this.spanZ = spanZ;
	}

	public static EchoChartModel of(EchoChartCloud cloud) {
		if (cloud.size() == 0) {
			return new EchoChartModel(1, 1, 1);
		}
		int lowX = Integer.MAX_VALUE, lowY = Integer.MAX_VALUE, lowZ = Integer.MAX_VALUE;
		int highX = Integer.MIN_VALUE, highY = Integer.MIN_VALUE, highZ = Integer.MIN_VALUE;
		for (int index = 0; index < cloud.size(); index++) {
			int packed = cloud.positions().getInt(index);
			lowX = Math.min(lowX, EchoCoordinates.unpackX(packed));
			highX = Math.max(highX, EchoCoordinates.unpackX(packed));
			lowY = Math.min(lowY, EchoCoordinates.unpackY(packed));
			highY = Math.max(highY, EchoCoordinates.unpackY(packed));
			lowZ = Math.min(lowZ, EchoCoordinates.unpackZ(packed));
			highZ = Math.max(highZ, EchoCoordinates.unpackZ(packed));
		}
		int width = highX - lowX + 1;
		int height = highY - lowY + 1;
		int depth = highZ - lowZ + 1;
		int longest = Math.max(width, Math.max(height, depth));
		int resolution = Math.min(GRID, longest);
		EchoChartModel model = new EchoChartModel(
				Math.max(1, width * resolution / longest),
				Math.max(1, height * resolution / longest),
				Math.max(1, depth * resolution / longest));

		Int2LongMap sums = new Int2LongOpenHashMap();
		Int2IntMap counts = new Int2IntOpenHashMap();
		BlockPos anchor = Minecraft.getInstance().player != null
				? Minecraft.getInstance().player.blockPosition() : BlockPos.ZERO;
		for (int index = 0; index < cloud.size(); index++) {
			int packed = cloud.positions().getInt(index);
			int key = pack(
					(EchoCoordinates.unpackX(packed) - lowX) * model.spanX / width,
					(EchoCoordinates.unpackY(packed) - lowY) * model.spanY / height,
					(EchoCoordinates.unpackZ(packed) - lowZ) * model.spanZ / depth);
			BlockState state = cloud.stateAt(index);
			int colour = state.getMapColor(Minecraft.getInstance().level, anchor).col;
			sums.put(key, sums.get(key) + channels(colour));
			counts.put(key, counts.get(key) + 1);
		}
		for (Int2IntMap.Entry entry : counts.int2IntEntrySet()) {
			int key = entry.getIntKey();
			model.cells.add(key);
			model.colours.add(average(sums.get(key), entry.getIntValue()));
			model.occupied.add(key);
		}
		return model;
	}

	private static long channels(int colour) {
		return (long) ((colour >> 16) & BYTE_MASK)
				| ((long) ((colour >> 8) & BYTE_MASK) << CHANNEL_BITS)
				| ((long) (colour & BYTE_MASK) << (CHANNEL_BITS * 2));
	}

	private static int average(long packed, int count) {
		int divisor = Math.max(1, count);
		int red = (int) ((packed & CHANNEL_MASK) / divisor);
		int green = (int) (((packed >> CHANNEL_BITS) & CHANNEL_MASK) / divisor);
		int blue = (int) (((packed >> (CHANNEL_BITS * 2)) & CHANNEL_MASK) / divisor);
		return (Math.min(BYTE_MASK, red) << 16) | (Math.min(BYTE_MASK, green) << 8)
				| Math.min(BYTE_MASK, blue);
	}

	public static int pack(int x, int y, int z) {
		return (x & GRID_MASK) | ((y & GRID_MASK) << GRID_BITS) | ((z & GRID_MASK) << (GRID_BITS * 2));
	}

	public static int cellX(int packed) {
		return packed & GRID_MASK;
	}

	public static int cellY(int packed) {
		return (packed >> GRID_BITS) & GRID_MASK;
	}

	public static int cellZ(int packed) {
		return (packed >> (GRID_BITS * 2)) & GRID_MASK;
	}

	public int size() {
		return cells.size();
	}

	public int cellAt(int index) {
		return cells.getInt(index);
	}

	public int colourAt(int index) {
		return colours.getInt(index);
	}

	public boolean holds(int x, int y, int z) {
		return occupied.contains(pack(x, y, z));
	}

	public int spanX() {
		return spanX;
	}

	public int spanY() {
		return spanY;
	}

	public int spanZ() {
		return spanZ;
	}

	public int widest() {
		return Math.max(spanX, Math.max(spanY, spanZ));
	}
}
