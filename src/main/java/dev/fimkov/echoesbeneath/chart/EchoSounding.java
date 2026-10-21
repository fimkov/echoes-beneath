package dev.fimkov.echoesbeneath.chart;

import dev.fimkov.echoesbeneath.config.EchoConfig;
import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import dev.fimkov.echoesbeneath.memory.EchoMemoryRecord;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockPalette;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockSnapshot;
import dev.fimkov.echoesbeneath.memory.timeline.SoundTimeline;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.world.level.block.Blocks;

public final class EchoSounding {
	private static final int MIN_CARRY = 2;
	private static final int[][] STEPS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	private EchoSounding() {
	}

	public static EchoChartShape of(EchoMemoryRecord record, int minY, int maxY, int budget) {
		return of(record, reach(record), minY, maxY, budget);
	}

	public static Columns reach(EchoMemoryRecord record) {
		Columns columns = new Columns(record.radius(), record.verticalRadius(), record.snapshot());
		seed(record.soundTimeline(), columns, record.radius());
		columns.flood();

		return columns;
	}

	public static EchoChartShape of(EchoMemoryRecord record, Columns columns, int minY, int maxY,
			int budget) {
		return of(record, columns, minY, maxY, budget, EchoConfig.values().sphericalCharts());
	}

	public static EchoChartShape of(EchoMemoryRecord record, Columns columns, int minY, int maxY,
			int budget, boolean spherical) {
		BlockSnapshot snapshot = record.snapshot();
		int vertical = record.verticalRadius();
		int lowest = Math.max(-vertical, minY - record.center().getY());
		int highest = Math.min(vertical, maxY - record.center().getY());

		if (lowest > highest || budget <= 0) {
			return EchoChartShape.empty(record.id(), lowest, highest, record.recordedVision());
		}

		IntList positions = new IntArrayList();
		IntList states = new IntArrayList();
		gather(snapshot, seeThrough(snapshot.palette()), columns, record.radius(), lowest, highest,
				budget, positions, states, spherical);
		BlockPalette silhouettes = new BlockPalette();
		for (int index = 0; index < states.size(); index++) {
			var state = snapshot.palette().stateAt(states.getInt(index));
			states.set(index, silhouettes.indexOf(state.isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO)
					? Blocks.STONE.defaultBlockState() : state));
		}

		return new EchoChartShape(record.id(), silhouettes, positions, states, lowest, highest,
				record.recordedVision());
	}

	private static void seed(SoundTimeline sounds, Columns columns, int radius) {
		for (int index = 0; index < sounds.size(); index++) {
			if (sounds.isStopAt(index)) {
				continue;
			}
			int x = (int) Math.floor(sounds.relativeXAt(index));
			int z = (int) Math.floor(sounds.relativeZAt(index));
			int carry = Math.clamp(Math.round(sounds.volumeAt(index)
							* EchoConfig.values().soundCarryBlocks()),
					MIN_CARRY, radius);
			columns.raise(x, z, carry);
		}
	}

	private static boolean[] seeThrough(BlockPalette palette) {
		boolean[] clear = new boolean[palette.size()];

		for (int index = 0; index < clear.length; index++) {
			clear[index] = !palette.stateAt(index).isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO);
		}

		return clear;
	}

	private static void gather(BlockSnapshot snapshot, boolean[] seen, Columns columns, int radius,
			int lowest, int highest, int budget, IntList positions, IntList states, boolean spherical) {
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				if (!columns.charted(x, z)) {
					continue;
				}
				for (int y = lowest; y <= highest; y++) {
					if (spherical && x * x + y * y + z * z > radius * radius) {
						continue;
					}
					if (positions.size() >= budget) {
						return;
					}
					int index = snapshot.paletteIndexAt(x, y, z);

					if (index == BlockPalette.AIR_INDEX || !onSurface(snapshot, seen, x, y, z)) {
						continue;
					}
					positions.add(EchoCoordinates.packRelative(x, y, z));
					states.add(index);
				}
			}
		}
	}

	private static boolean onSurface(BlockSnapshot snapshot, boolean[] seen, int x, int y, int z) {
		return clear(snapshot, seen, x + 1, y, z) || clear(snapshot, seen, x - 1, y, z)
				|| clear(snapshot, seen, x, y + 1, z) || clear(snapshot, seen, x, y - 1, z)
				|| clear(snapshot, seen, x, y, z + 1) || clear(snapshot, seen, x, y, z - 1);
	}

	private static boolean clear(BlockSnapshot snapshot, boolean[] seen, int x, int y, int z) {
		int index = snapshot.paletteIndexAt(x, y, z);

		return index < 0 || index >= seen.length || seen[index];
	}

	public static final class Columns {
		private final int radius;
		private final int width;
		private final int[] carried;
		private final boolean[] open;
		private final IntList[] fronts;
		private int peak;

		private Columns(int radius, int vertical, BlockSnapshot snapshot) {
			this.radius = radius;
			this.width = radius * 2 + 1;
			this.carried = new int[width * width];
			this.open = new boolean[width * width];
			this.fronts = new IntList[radius + 2];

			for (int x = -radius; x <= radius; x++) {
				for (int z = -radius; z <= radius; z++) {
					open[slot(x, z)] = hasAir(snapshot, x, z, vertical);
				}
			}
		}

		private static boolean hasAir(BlockSnapshot snapshot, int x, int z, int vertical) {
			for (int y = -vertical; y <= vertical; y++) {
				if (snapshot.paletteIndexAt(x, y, z) == BlockPalette.AIR_INDEX) {
					return true;
				}
			}
			return false;
		}

		private boolean holds(int x, int z) {
			return x >= -radius && x <= radius && z >= -radius && z <= radius;
		}

		private int slot(int x, int z) {
			return (z + radius) * width + (x + radius);
		}

		private boolean reached(int x, int z) {
			return holds(x, z) && carried[slot(x, z)] > 0;
		}

		private boolean charted(int x, int z) {
			if (reached(x, z)) {
				return true;
			}
			for (int[] step : STEPS) {
				if (reached(x + step[0], z + step[1])) {
					return true;
				}
			}
			return false;
		}

		private void raise(int x, int z, int carry) {
			if (carry <= 0 || !holds(x, z) || !open[slot(x, z)]) {
				return;
			}
			int at = slot(x, z);

			if (carried[at] >= carry) {
				return;
			}
			carried[at] = carry;

			if (fronts[carry] == null) {
				fronts[carry] = new IntArrayList();
			}
			fronts[carry].add(at);
			peak = Math.max(peak, carry);
		}

		private void flood() {
			for (int carry = peak; carry > 0; carry--) {
				IntList front = fronts[carry];

				if (front == null) {
					continue;
				}
				for (int cursor = 0; cursor < front.size(); cursor++) {
					int at = front.getInt(cursor);

					if (carried[at] != carry) {
						continue;
					}
					int x = at % width - radius;
					int z = at / width - radius;

					for (int[] step : STEPS) {
						raise(x + step[0], z + step[1], carry - 1);
					}
				}
			}
		}
	}
}
