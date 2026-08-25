package dev.fimkov.echoesbeneath.client.render;

import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockPalette;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockSnapshot;
import dev.fimkov.echoesbeneath.memory.snapshot.SnapshotSection;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class EchoSectionMesh {
	private static final int SECTION_SIZE = SnapshotSection.SECTION_SIZE;
	public static final double CIRCUMRADIUS = SECTION_SIZE * 0.5D * Math.sqrt(3.0D);

	private final int originX;
	private final int originY;
	private final int originZ;
	private final int[] faces;

	private EchoSectionMesh(int originX, int originY, int originZ, int[] faces) {
		this.originX = originX;
		this.originY = originY;
		this.originZ = originZ;
		this.faces = faces;
	}

	public static EchoSectionMesh build(BlockSnapshot snapshot, int sectionKey, IntSet excludedPositions) {
		int originX = BlockSnapshot.sectionKeyX(sectionKey) * SECTION_SIZE;
		int originY = BlockSnapshot.sectionKeyY(sectionKey) * SECTION_SIZE;
		int originZ = BlockSnapshot.sectionKeyZ(sectionKey) * SECTION_SIZE;
		IntArrayList collected = new IntArrayList();
		for (int localY = 0; localY < SECTION_SIZE; localY++) {
			for (int localZ = 0; localZ < SECTION_SIZE; localZ++) {
				for (int localX = 0; localX < SECTION_SIZE; localX++) {
					int relativeX = originX + localX;
					int relativeY = originY + localY;
					int relativeZ = originZ + localZ;
					if (snapshot.paletteIndexAt(relativeX, relativeY, relativeZ) == BlockPalette.AIR_INDEX) {
						continue;
					}
					if (!snapshot.stateAt(relativeX, relativeY, relativeZ).canOcclude()) {
						continue;
					}
					if (excludedPositions.contains(EchoCoordinates.packRelative(relativeX, relativeY, relativeZ))) {
						continue;
					}
					for (Direction direction : Direction.values()) {
						if (!hasFace(snapshot, relativeX, relativeY, relativeZ, direction)) {
							continue;
						}
						int edgeMask = 0;
						for (int edge = 0; edge < EchoFaceGeometry.EDGES_PER_FACE; edge++) {
							Direction step = EchoFaceGeometry.edgeNeighbour(direction, edge);
							if (!hasFace(snapshot,
									relativeX + step.getStepX(),
									relativeY + step.getStepY(),
									relativeZ + step.getStepZ(), direction)) {
								edgeMask |= 1 << edge;
							}
						}
						collected.add(EchoFaceGeometry.withEdges(
								EchoFaceGeometry.pack(localX, localY, localZ, direction), edgeMask));
					}
				}
			}
		}
		return new EchoSectionMesh(originX, originY, originZ, collected.toIntArray());
	}

	private static boolean solid(BlockSnapshot snapshot, int x, int y, int z) {
		if (snapshot.paletteIndexAt(x, y, z) == BlockPalette.AIR_INDEX) {
			return false;
		}
		return snapshot.stateAt(x, y, z).canOcclude();
	}

	private static boolean hasFace(BlockSnapshot snapshot, int x, int y, int z, Direction direction) {
		return solid(snapshot, x, y, z)
				&& !solid(snapshot, x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ());
	}

	public boolean isEmpty() {
		return faces.length == 0;
	}

	public int faceCount() {
		return faces.length;
	}

	public int[] faces() {
		return faces;
	}

	public int originX() {
		return originX;
	}

	public int originY() {
		return originY;
	}

	public int originZ() {
		return originZ;
	}

	public AABB boundsAround(double centerX, double centerY, double centerZ) {
		return new AABB(
				centerX + originX, centerY + originY, centerZ + originZ,
				centerX + originX + SECTION_SIZE, centerY + originY + SECTION_SIZE, centerZ + originZ + SECTION_SIZE);
	}
}
