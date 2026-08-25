package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockPalette;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockSnapshot;
import dev.fimkov.echoesbeneath.memory.snapshot.SnapshotSection;
import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class EchoSectionMesh implements AutoCloseable {
	private static final int SECTION_SIZE = SnapshotSection.SECTION_SIZE;
	private static final int OPAQUE_ALPHA = 0xFF000000;

	private final int originX;
	private final int originY;
	private final int originZ;
	private final int[] faces;
	private VertexBuffer buffer;
	private boolean uploaded;

	private EchoSectionMesh(int originX, int originY, int originZ, int[] faces) {
		this.originX = originX;
		this.originY = originY;
		this.originZ = originZ;
		this.faces = faces;
	}

	public static EchoSectionMesh build(BlockSnapshot snapshot, int sectionKey, IntSet excludedPositions) {
		int sectionX = BlockSnapshot.sectionKeyX(sectionKey);
		int sectionY = BlockSnapshot.sectionKeyY(sectionKey);
		int sectionZ = BlockSnapshot.sectionKeyZ(sectionKey);
		int originX = sectionX * SECTION_SIZE;
		int originY = sectionY * SECTION_SIZE;
		int originZ = sectionZ * SECTION_SIZE;
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
					if (excludedPositions.contains(EchoCoordinates.packRelative(relativeX, relativeY, relativeZ))) {
						continue;
					}
					for (Direction direction : Direction.values()) {
						int neighbourIndex = snapshot.paletteIndexAt(
								relativeX + direction.getStepX(),
								relativeY + direction.getStepY(),
								relativeZ + direction.getStepZ());
						if (neighbourIndex == BlockPalette.AIR_INDEX) {
							collected.add(EchoFaceGeometry.pack(localX, localY, localZ, direction));
						}
					}
				}
			}
		}
		return new EchoSectionMesh(originX, originY, originZ, collected.toIntArray());
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

	public VertexBuffer uploadedBuffer() {
		if (uploaded) {
			return buffer;
		}
		uploaded = true;
		if (faces.length == 0) {
			return null;
		}
		BufferBuilder builder = Tesselator.getInstance()
				.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int packedFace : faces) {
			int color = OPAQUE_ALPHA | EchoFaceGeometry.shadeColor(EchoFaceGeometry.direction(packedFace));
			EchoFaceGeometry.emit(builder, null, 0.0F, 0.0F, 0.0F, packedFace, color);
		}
		MeshData mesh = builder.build();
		if (mesh == null) {
			return null;
		}
		buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
		buffer.bind();
		buffer.upload(mesh);
		VertexBuffer.unbind();
		return buffer;
	}

	@Override
	public void close() {
		if (buffer != null) {
			buffer.close();
			buffer = null;
		}
		uploaded = true;
	}
}
