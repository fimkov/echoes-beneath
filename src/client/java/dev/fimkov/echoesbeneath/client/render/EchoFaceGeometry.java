package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

public final class EchoFaceGeometry {
	public static final int LOCAL_BITS = 4;
	public static final int LOCAL_MASK = (1 << LOCAL_BITS) - 1;
	public static final int DIRECTION_SHIFT = LOCAL_BITS * 3;
	public static final int EDGE_SHIFT = DIRECTION_SHIFT + 3;
	public static final int EDGES_PER_FACE = 4;
	public static final int CORNERS_PER_FACE = 4;
	public static final int COMPONENTS_PER_CORNER = 3;

	private static final Direction[] DIRECTIONS = Direction.values();
	private static final float FACE_INSET = 0.002F;

	private EchoFaceGeometry() {
	}

	public static int pack(int localX, int localY, int localZ, Direction direction) {
		return (localX & LOCAL_MASK)
				| ((localY & LOCAL_MASK) << LOCAL_BITS)
				| ((localZ & LOCAL_MASK) << (LOCAL_BITS * 2))
				| (direction.ordinal() << DIRECTION_SHIFT);
	}

	public static int withEdges(int packed, int edgeMask) {
		return packed | ((edgeMask & 0xF) << EDGE_SHIFT);
	}

	public static int edgeMask(int packed) {
		return (packed >> EDGE_SHIFT) & 0xF;
	}

	public static Direction tangentA(Direction face) {
		return switch (face) {
			case DOWN, SOUTH -> Direction.EAST;
			case UP, WEST -> Direction.SOUTH;
			case NORTH, EAST -> Direction.UP;
		};
	}

	public static Direction tangentB(Direction face) {
		return switch (face) {
			case DOWN, EAST -> Direction.SOUTH;
			case UP, NORTH -> Direction.EAST;
			case SOUTH, WEST -> Direction.UP;
		};
	}

	public static Direction edgeNeighbour(Direction face, int edgeIndex) {
		return switch (edgeIndex) {
			case 0 -> tangentB(face).getOpposite();
			case 1 -> tangentA(face);
			case 2 -> tangentB(face);
			default -> tangentA(face).getOpposite();
		};
	}

	public static void edgeEndpoints(int packedFace, float originX, float originY, float originZ,
			int edgeIndex, float[] basis, float[] endpoints) {
		faceBasis(packedFace, originX, originY, originZ, basis);
		float[] corner = new float[12];

		for (int index = 0; index < CORNERS_PER_FACE; index++) {
			float u = index == 1 || index == 2 ? 1.0F : 0.0F;
			float v = index >= 2 ? 1.0F : 0.0F;
			corner[index * 3] = basis[0] + basis[3] * u + basis[6] * v;
			corner[index * 3 + 1] = basis[1] + basis[4] * u + basis[7] * v;
			corner[index * 3 + 2] = basis[2] + basis[5] * u + basis[8] * v;
		}

		int from = edgeIndex;
		int to = (edgeIndex + 1) % CORNERS_PER_FACE;
		System.arraycopy(corner, from * 3, endpoints, 0, 3);
		System.arraycopy(corner, to * 3, endpoints, 3, 3);
	}

	public static int localX(int packed) {
		return packed & LOCAL_MASK;
	}

	public static int localY(int packed) {
		return (packed >> LOCAL_BITS) & LOCAL_MASK;
	}

	public static int localZ(int packed) {
		return (packed >> (LOCAL_BITS * 2)) & LOCAL_MASK;
	}

	public static Direction direction(int packed) {
		return DIRECTIONS[(packed >> DIRECTION_SHIFT) & 0x7];
	}

	public static void faceBasis(int packedFace, float originX, float originY, float originZ, float[] basis) {
		float x = originX + localX(packedFace);
		float y = originY + localY(packedFace);
		float z = originZ + localZ(packedFace);

		switch (direction(packedFace)) {
			case DOWN -> writeBasis(basis, x, y - FACE_INSET, z, 1, 0, 0, 0, 0, 1);
			case UP -> writeBasis(basis, x, y + 1 + FACE_INSET, z, 0, 0, 1, 1, 0, 0);
			case NORTH -> writeBasis(basis, x, y, z - FACE_INSET, 0, 1, 0, 1, 0, 0);
			case SOUTH -> writeBasis(basis, x, y, z + 1 + FACE_INSET, 1, 0, 0, 0, 1, 0);
			case WEST -> writeBasis(basis, x - FACE_INSET, y, z, 0, 0, 1, 0, 1, 0);
			case EAST -> writeBasis(basis, x + 1 + FACE_INSET, y, z, 0, 1, 0, 0, 0, 1);
		}
	}

	private static void writeBasis(float[] basis, float x, float y, float z,
			float ax, float ay, float az, float bx, float by, float bz) {
		basis[0] = x;
		basis[1] = y;
		basis[2] = z;
		basis[3] = ax;
		basis[4] = ay;
		basis[5] = az;
		basis[6] = bx;
		basis[7] = by;
		basis[8] = bz;
	}

	public static void cornerOffsets(int packedFace, float originX, float originY, float originZ, float[] corners) {
		float x = originX + localX(packedFace);
		float y = originY + localY(packedFace);
		float z = originZ + localZ(packedFace);

		switch (direction(packedFace)) {
			case DOWN -> writeQuad(corners, x, y - FACE_INSET, z, 1, 0, 0, 0, 0, 1);
			case UP -> writeQuad(corners, x, y + 1 + FACE_INSET, z, 0, 0, 1, 1, 0, 0);
			case NORTH -> writeQuad(corners, x, y, z - FACE_INSET, 0, 1, 0, 1, 0, 0);
			case SOUTH -> writeQuad(corners, x, y, z + 1 + FACE_INSET, 1, 0, 0, 0, 1, 0);
			case WEST -> writeQuad(corners, x - FACE_INSET, y, z, 0, 0, 1, 0, 1, 0);
			case EAST -> writeQuad(corners, x + 1 + FACE_INSET, y, z, 0, 1, 0, 0, 0, 1);
		}
	}

	private static void writeQuad(float[] corners, float x, float y, float z,
			float ax, float ay, float az, float bx, float by, float bz) {
		corners[0] = x;
		corners[1] = y;
		corners[2] = z;
		corners[3] = x + ax;
		corners[4] = y + ay;
		corners[5] = z + az;
		corners[6] = x + ax + bx;
		corners[7] = y + ay + by;
		corners[8] = z + az + bz;
		corners[9] = x + bx;
		corners[10] = y + by;
		corners[11] = z + bz;
	}

	public static void emitVertex(VertexConsumer consumer, Matrix4f matrix,
			float x, float y, float z, float u, float v, int argb) {
		consumer.addVertex(matrix, x, y, z)
				.setUv(u, v)
				.setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
	}

	public static void emitGradient(VertexConsumer consumer, Matrix4f matrix, float[] corners, int[] cornerColors) {
		for (int corner = 0; corner < CORNERS_PER_FACE; corner++) {
			int argb = cornerColors[corner];
			consumer.addVertex(matrix,
							corners[corner * COMPONENTS_PER_CORNER],
							corners[corner * COMPONENTS_PER_CORNER + 1],
							corners[corner * COMPONENTS_PER_CORNER + 2])
					.setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
		}
	}
}
