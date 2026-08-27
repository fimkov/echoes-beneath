package dev.fimkov.echoesbeneath.client.chart;

import dev.fimkov.echoesbeneath.chart.EchoChartCloud;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class EchoChartModel {
	public static final int GRID = 32;
	private static final int GRID_BITS = 6;
	private static final int GRID_MASK = (1 << GRID_BITS) - 1;
	private static final float DIMMEST = 0.42F;

	private final IntList cells = new IntArrayList();
	private final List<BlockState> states = new ArrayList<>();
	private final IntSet occupied = new IntOpenHashSet();
	private final IntSet opaque = new IntOpenHashSet();
	private final IntSet wet = new IntOpenHashSet();
	private final int spanX;
	private final int spanY;
	private final int spanZ;
	private int recordedVision;

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
		model.recordedVision = cloud.recordedVision();

		Int2ObjectMap<Int2IntMap> votes = new Int2ObjectOpenHashMap<>();

		for (int index = 0; index < cloud.size(); index++) {
			int packed = cloud.positions().getInt(index);
			int key = pack(
					(EchoCoordinates.unpackX(packed) - lowX) * model.spanX / width,
					(EchoCoordinates.unpackY(packed) - lowY) * model.spanY / height,
					(EchoCoordinates.unpackZ(packed) - lowZ) * model.spanZ / depth);
			Int2IntMap tally = votes.computeIfAbsent(key, ignored -> new Int2IntOpenHashMap());
			int state = cloud.states().getInt(index);
			tally.put(state, tally.get(state) + 1);
		}

		for (Int2ObjectMap.Entry<Int2IntMap> entry : votes.int2ObjectEntrySet()) {
			BlockState state = cloud.paletteAt(winner(entry.getValue()));
			model.cells.add(entry.getIntKey());
			model.states.add(state);
			model.occupied.add(entry.getIntKey());

			if (state.isSolidRender()) {
				model.opaque.add(entry.getIntKey());
			}

			if (!state.getFluidState().isEmpty()) {
				model.wet.add(entry.getIntKey());
			}
		}

		return model;
	}

	private static int winner(Int2IntMap tally) {
		int best = 0;
		int bestCount = -1;

		for (Int2IntMap.Entry vote : tally.int2IntEntrySet()) {
			if (vote.getIntValue() > bestCount) {
				bestCount = vote.getIntValue();
				best = vote.getIntKey();
			}
		}

		return best;
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

	public BlockState stateAt(int index) {
		return states.get(index);
	}

	public boolean holds(int x, int y, int z) {
		return occupied.contains(pack(x, y, z));
	}

	// Glass, water, leaves and grass are neighbours you can see straight through,
	// so they do not count as walls when deciding what to leave out.
	public boolean blocks(int x, int y, int z) {
		return opaque.contains(pack(x, y, z));
	}

	public boolean wet(int x, int y, int z) {
		return wet.contains(pack(x, y, z));
	}

	public boolean isBuried(int packed) {
		int x = cellX(packed);
		int y = cellY(packed);
		int z = cellZ(packed);

		return blocks(x + 1, y, z) && blocks(x - 1, y, z) && blocks(x, y + 1, z)
				&& blocks(x, y - 1, z) && blocks(x, y, z + 1) && blocks(x, y, z - 1);
	}

	public float brightness() {
		int vision = recordedVision < 0 ? EchoLensUpgrades.MAX_VISION : recordedVision;

		return DIMMEST + (1.0F - DIMMEST) * vision / EchoLensUpgrades.MAX_VISION;
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
