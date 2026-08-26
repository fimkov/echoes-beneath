package dev.fimkov.echoesbeneath.chart;

import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import dev.fimkov.echoesbeneath.memory.EchoMemoryRecord;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockPalette;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockSnapshot;
import dev.fimkov.echoesbeneath.memory.timeline.SoundTimeline;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public final class EchoSounding {
	public static final int BLOCKS_PER_VOLUME = 16;
	private static final int MIN_CARRY = 2;
	private static final int[][] STEPS = {
			{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

	private EchoSounding() {
	}

	public static EchoChartShape of(EchoMemoryRecord record, int minY, int maxY, int budget) {
		BlockSnapshot snapshot = record.snapshot();
		int radius = record.radius();
		int vertical = record.verticalRadius();
		int lowest = Math.max(-vertical, minY - record.center().getY());
		int highest = Math.min(vertical, maxY - record.center().getY());
		if (lowest > highest || budget <= 0) {
			return EchoChartShape.empty(record.id(), lowest, highest, record.recordedVision());
		}

		Reach reach = new Reach(radius, vertical);
		seed(record.soundTimeline(), snapshot, reach, radius);
		IntSet surfaces = flood(snapshot, reach, lowest, highest);

		IntList positions = new IntArrayList();
		IntList states = new IntArrayList();
		for (int packed : surfaces) {
			if (positions.size() >= budget) {
				break;
			}
			int index = snapshot.paletteIndexAt(EchoCoordinates.unpackX(packed),
					EchoCoordinates.unpackY(packed), EchoCoordinates.unpackZ(packed));
			if (index == BlockPalette.AIR_INDEX) {
				continue;
			}
			positions.add(packed);
			states.add(index);
		}
		return new EchoChartShape(record.id(), snapshot.palette(), positions, states, lowest, highest,
				record.recordedVision());
	}

	private static void seed(SoundTimeline sounds, BlockSnapshot snapshot, Reach reach, int radius) {
		for (int index = 0; index < sounds.size(); index++) {
			int x = (int) Math.floor(sounds.relativeXAt(index));
			int y = (int) Math.floor(sounds.relativeYAt(index));
			int z = (int) Math.floor(sounds.relativeZAt(index));
			int carry = Math.clamp(Math.round(sounds.volumeAt(index) * BLOCKS_PER_VOLUME),
					MIN_CARRY, radius);
			if (snapshot.paletteIndexAt(x, y, z) != BlockPalette.AIR_INDEX) {
				continue;
			}
			reach.raise(x, y, z, carry);
		}
	}

	private static IntSet flood(BlockSnapshot snapshot, Reach reach, int lowest, int highest) {
		IntSet surfaces = new IntOpenHashSet();
		for (int carry = reach.peak(); carry > 0; carry--) {
			IntList front = reach.at(carry);
			if (front == null) {
				continue;
			}
			for (int cursor = 0; cursor < front.size(); cursor++) {
				int packed = front.getInt(cursor);
				if (reach.of(packed) != carry) {
					continue;
				}
				int x = EchoCoordinates.unpackX(packed);
				int y = EchoCoordinates.unpackY(packed);
				int z = EchoCoordinates.unpackZ(packed);
				for (int[] step : STEPS) {
					int nextX = x + step[0];
					int nextY = y + step[1];
					int nextZ = z + step[2];
					if (!reach.holds(nextX, nextY, nextZ)) {
						continue;
					}
					if (snapshot.paletteIndexAt(nextX, nextY, nextZ) != BlockPalette.AIR_INDEX) {
						if (nextY >= lowest && nextY <= highest) {
							surfaces.add(EchoCoordinates.packRelative(nextX, nextY, nextZ));
						}
						continue;
					}
					reach.raise(nextX, nextY, nextZ, carry - 1);
				}
			}
		}
		return surfaces;
	}

	private static final class Reach {
		private final int radius;
		private final int vertical;
		private final int width;
		private final int height;
		private final int[] carried;
		private final IntList[] fronts;
		private int peak;

		private Reach(int radius, int vertical) {
			this.radius = radius;
			this.vertical = vertical;
			this.width = radius * 2 + 1;
			this.height = vertical * 2 + 1;
			this.carried = new int[width * height * width];
			this.fronts = new IntList[radius + 2];
		}

		private boolean holds(int x, int y, int z) {
			return x >= -radius && x <= radius && z >= -radius && z <= radius
					&& y >= -vertical && y <= vertical;
		}

		private int slot(int x, int y, int z) {
			return ((y + vertical) * width + (z + radius)) * width + (x + radius);
		}

		private int of(int packed) {
			return carried[slot(EchoCoordinates.unpackX(packed), EchoCoordinates.unpackY(packed),
					EchoCoordinates.unpackZ(packed))];
		}

		private void raise(int x, int y, int z, int carry) {
			if (carry <= 0 || !holds(x, y, z)) {
				return;
			}
			int at = slot(x, y, z);
			if (carried[at] >= carry) {
				return;
			}
			carried[at] = carry;
			if (fronts[carry] == null) {
				fronts[carry] = new IntArrayList();
			}
			fronts[carry].add(EchoCoordinates.packRelative(x, y, z));
			peak = Math.max(peak, carry);
		}

		private int peak() {
			return peak;
		}

		private IntList at(int carry) {
			return fronts[carry];
		}
	}
}
