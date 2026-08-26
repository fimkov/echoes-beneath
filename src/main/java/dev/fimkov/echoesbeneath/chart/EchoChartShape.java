package dev.fimkov.echoesbeneath.chart;

import dev.fimkov.echoesbeneath.memory.snapshot.BlockPalette;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.UUID;

public record EchoChartShape(UUID memoryId, BlockPalette palette, IntList positions, IntList states,
		int lowestRelativeY, int highestRelativeY, int recordedVision) {

	public static EchoChartShape empty(UUID memoryId, int lowestRelativeY, int highestRelativeY,
			int recordedVision) {
		return new EchoChartShape(memoryId, new BlockPalette(), new IntArrayList(), new IntArrayList(),
				lowestRelativeY, highestRelativeY, recordedVision);
	}

	public int size() {
		return positions.size();
	}

	public boolean isEmpty() {
		return positions.isEmpty();
	}
}
