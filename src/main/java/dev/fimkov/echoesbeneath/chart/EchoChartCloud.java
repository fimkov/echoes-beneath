package dev.fimkov.echoesbeneath.chart;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public record EchoChartCloud(List<BlockState> palette, IntList positions, IntList states,
		int lowestRelativeY, int highestRelativeY, int recordedVision) {

	public int size() {
		return positions.size();
	}

	public BlockState stateAt(int index) {
		int paletteIndex = states.getInt(index);
		return paletteIndex >= 0 && paletteIndex < palette.size()
				? palette.get(paletteIndex)
				: palette.getFirst();
	}
}
