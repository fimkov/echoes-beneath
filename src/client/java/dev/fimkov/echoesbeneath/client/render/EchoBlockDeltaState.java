package dev.fimkov.echoesbeneath.client.render;

import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockPalette;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockSnapshot;
import dev.fimkov.echoesbeneath.memory.timeline.BlockTimeline;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public final class EchoBlockDeltaState {
	private final BlockSnapshot snapshot;
	private final BlockTimeline timeline;
	private final IntSet trackedPositions = new IntOpenHashSet();
	private final Int2IntMap currentStates = new Int2IntOpenHashMap();
	private int appliedEvents;

	public EchoBlockDeltaState(BlockSnapshot snapshot, BlockTimeline timeline) {
		this.snapshot = snapshot;
		this.timeline = timeline;
		for (int index = 0; index < timeline.size(); index++) {
			trackedPositions.add(timeline.packedPositionAt(index));
		}
		resetToStart();
	}

	public IntSet trackedPositions() {
		return trackedPositions;
	}

	public Int2IntMap currentStates() {
		return currentStates;
	}

	private void resetToStart() {
		currentStates.clear();
		appliedEvents = 0;
		for (int packed : trackedPositions) {
			currentStates.put(packed, snapshot.paletteIndexAt(
					EchoCoordinates.unpackX(packed),
					EchoCoordinates.unpackY(packed),
					EchoCoordinates.unpackZ(packed)));
		}
	}

	public void advanceTo(int tick) {
		if (appliedEvents > 0 && timeline.tickAt(appliedEvents - 1) > tick) {
			resetToStart();
		}
		while (appliedEvents < timeline.size() && timeline.tickAt(appliedEvents) <= tick) {
			currentStates.put(timeline.packedPositionAt(appliedEvents), timeline.newStateAt(appliedEvents));
			appliedEvents++;
		}
	}

	public boolean isSolidAt(int packedPosition) {
		int index = currentStates.getOrDefault(packedPosition, BlockPalette.AIR_INDEX);
		return index != BlockPalette.AIR_INDEX;
	}

	public boolean isSolidAt(int relativeX, int relativeY, int relativeZ) {
		int packed = EchoCoordinates.packRelative(relativeX, relativeY, relativeZ);
		if (trackedPositions.contains(packed)) {
			return isSolidAt(packed);
		}
		return snapshot.paletteIndexAt(relativeX, relativeY, relativeZ) != BlockPalette.AIR_INDEX;
	}
}
