package dev.fimkov.echoesbeneath.recording;

import dev.fimkov.echoesbeneath.memory.snapshot.BlockPalette;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockSnapshot;
import dev.fimkov.echoesbeneath.memory.snapshot.SnapshotSection;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class SnapshotCapture {
	private static final int SECTIONS_PER_TICK = 16;
	private static final int SECTION_SIZE = SnapshotSection.SECTION_SIZE;

	private final ServerLevel level;
	private final BlockPos center;
	private final int radius;
	private final int minRelativeY;
	private final int maxRelativeY;
	private final BlockPalette palette;
	private final Int2ObjectMap<SnapshotSection> sections = new Int2ObjectOpenHashMap<>();
	private final List<int[]> pendingSections = new ArrayList<>();
	private final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();
	private final Int2ObjectMap<BlockState> preChangeOverrides = new Int2ObjectOpenHashMap<>();
	private final IntSet capturedSections = new IntOpenHashSet();
	private int cursor;

	public SnapshotCapture(ServerLevel level, BlockPos center, int radius, int verticalRadius, BlockPalette palette) {
		this.level = level;
		this.center = center;
		this.radius = radius;
		this.palette = palette;
		this.minRelativeY = Math.max(level.getMinBuildHeight() - center.getY(), -verticalRadius);
		this.maxRelativeY = Math.min(level.getMaxBuildHeight() - 1 - center.getY(), verticalRadius);
		queueSections();
	}

	private void queueSections() {
		int minSectionX = Math.floorDiv(-radius, SECTION_SIZE);
		int maxSectionX = Math.floorDiv(radius, SECTION_SIZE);
		int minSectionY = Math.floorDiv(minRelativeY, SECTION_SIZE);
		int maxSectionY = Math.floorDiv(maxRelativeY, SECTION_SIZE);

		for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
			for (int sectionZ = minSectionX; sectionZ <= maxSectionX; sectionZ++) {
				if (!sectionTouchesCylinder(sectionX, sectionZ)) {
					continue;
				}
				for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
					pendingSections.add(new int[]{sectionX, sectionY, sectionZ});
				}
			}
		}
	}

	private boolean sectionTouchesCylinder(int sectionX, int sectionZ) {
		int nearestX = nearestCoordinate(sectionX * SECTION_SIZE, sectionX * SECTION_SIZE + SECTION_SIZE - 1);
		int nearestZ = nearestCoordinate(sectionZ * SECTION_SIZE, sectionZ * SECTION_SIZE + SECTION_SIZE - 1);

		return (long) nearestX * nearestX + (long) nearestZ * nearestZ <= (long) radius * radius;
	}

	private static int nearestCoordinate(int low, int high) {
		if (low > 0) {
			return low;
		}

		if (high < 0) {
			return high;
		}

		return 0;
	}

	public void notePreChange(int relativeX, int relativeY, int relativeZ, BlockState previous) {
		int sectionKey = BlockSnapshot.sectionKey(
				Math.floorDiv(relativeX, SECTION_SIZE),
				Math.floorDiv(relativeY, SECTION_SIZE),
				Math.floorDiv(relativeZ, SECTION_SIZE));

		if (capturedSections.contains(sectionKey)) {
			return;
		}

		preChangeOverrides.putIfAbsent(EchoCoordinates.packRelative(relativeX, relativeY, relativeZ), previous);
	}

	public boolean isComplete() {
		return cursor >= pendingSections.size();
	}

	public float progress() {
		if (pendingSections.isEmpty()) {
			return 1.0F;
		}

		return (float) cursor / pendingSections.size();
	}

	public void advance() {
		int processed = 0;

		while (processed < SECTIONS_PER_TICK && !isComplete()) {
			int[] coordinates = pendingSections.get(cursor++);
			captureSection(coordinates[0], coordinates[1], coordinates[2]);
			processed++;
		}
	}

	public void completeNow() {
		while (!isComplete()) {
			int[] coordinates = pendingSections.get(cursor++);
			captureSection(coordinates[0], coordinates[1], coordinates[2]);
		}
	}

	private void captureSection(int sectionX, int sectionY, int sectionZ) {
		int[] cells = new int[SnapshotSection.CELLS_PER_SECTION];
		boolean anySolid = false;

		for (int localY = 0; localY < SECTION_SIZE; localY++) {
			int relativeY = sectionY * SECTION_SIZE + localY;

			if (relativeY < minRelativeY || relativeY > maxRelativeY) {
				continue;
			}
			for (int localZ = 0; localZ < SECTION_SIZE; localZ++) {
				int relativeZ = sectionZ * SECTION_SIZE + localZ;

				for (int localX = 0; localX < SECTION_SIZE; localX++) {
					int relativeX = sectionX * SECTION_SIZE + localX;

					if ((long) relativeX * relativeX + (long) relativeZ * relativeZ > (long) radius * radius) {
						continue;
					}
					scratch.set(center.getX() + relativeX, center.getY() + relativeY, center.getZ() + relativeZ);

					if (!level.hasChunkAt(scratch)) {
						continue;
					}
					BlockState state = preChangeOverrides.getOrDefault(
							EchoCoordinates.packRelative(relativeX, relativeY, relativeZ),
							level.getBlockState(scratch));

					if (state.isAir()) {
						continue;
					}
					cells[SnapshotSection.cellIndex(localX, localY, localZ)] = palette.indexOf(state);
					anySolid = true;
				}
			}
		}

		capturedSections.add(BlockSnapshot.sectionKey(sectionX, sectionY, sectionZ));

		if (!anySolid) {
			return;
		}

		sections.put(BlockSnapshot.sectionKey(sectionX, sectionY, sectionZ), SnapshotSection.compact(cells));
	}

	public BlockSnapshot toSnapshot() {
		return new BlockSnapshot(palette, sections, radius, minRelativeY, maxRelativeY);
	}

	public int minRelativeY() {
		return minRelativeY;
	}

	public int maxRelativeY() {
		return maxRelativeY;
	}
}
