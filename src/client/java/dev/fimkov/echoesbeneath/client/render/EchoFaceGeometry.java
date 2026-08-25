package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

public final class EchoFaceGeometry {
	public static final int LOCAL_BITS = 4;
	public static final int LOCAL_MASK = (1 << LOCAL_BITS) - 1;
	public static final int DIRECTION_SHIFT = LOCAL_BITS * 3;

	private static final Direction[] DIRECTIONS = Direction.values();
	private static final int[] SHADE_COLORS = new int[DIRECTIONS.length];
	private static final float FACE_INSET = 0.001F;

	static {
		SHADE_COLORS[Direction.DOWN.ordinal()] = 0x000000;
		SHADE_COLORS[Direction.UP.ordinal()] = 0x080A0E;
		SHADE_COLORS[Direction.NORTH.ordinal()] = 0x030407;
		SHADE_COLORS[Direction.SOUTH.ordinal()] = 0x040509;
		SHADE_COLORS[Direction.WEST.ordinal()] = 0x020305;
		SHADE_COLORS[Direction.EAST.ordinal()] = 0x05060B;
	}

	private EchoFaceGeometry() {
	}

	public static int pack(int localX, int localY, int localZ, Direction direction) {
		return (localX & LOCAL_MASK)
				| ((localY & LOCAL_MASK) << LOCAL_BITS)
				| ((localZ & LOCAL_MASK) << (LOCAL_BITS * 2))
				| (direction.ordinal() << DIRECTION_SHIFT);
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

	public static int shadeColor(Direction direction) {
		return SHADE_COLORS[direction.ordinal()];
	}

	public static void emit(VertexConsumer consumer, Matrix4f matrix, float originX, float originY, float originZ,
			int packedFace, int argb) {
		float x = originX + localX(packedFace);
		float y = originY + localY(packedFace);
		float z = originZ + localZ(packedFace);
		int alpha = (argb >>> 24) & 0xFF;
		int red = (argb >> 16) & 0xFF;
		int green = (argb >> 8) & 0xFF;
		int blue = argb & 0xFF;
		switch (direction(packedFace)) {
			case DOWN -> quad(consumer, matrix, x, y - FACE_INSET, z, 1, 0, 0, 0, 0, 1, red, green, blue, alpha);
			case UP -> quad(consumer, matrix, x, y + 1 + FACE_INSET, z, 0, 0, 1, 1, 0, 0, red, green, blue, alpha);
			case NORTH -> quad(consumer, matrix, x, y, z - FACE_INSET, 0, 1, 0, 1, 0, 0, red, green, blue, alpha);
			case SOUTH -> quad(consumer, matrix, x, y, z + 1 + FACE_INSET, 1, 0, 0, 0, 1, 0, red, green, blue, alpha);
			case WEST -> quad(consumer, matrix, x - FACE_INSET, y, z, 0, 0, 1, 0, 1, 0, red, green, blue, alpha);
			case EAST -> quad(consumer, matrix, x + 1 + FACE_INSET, y, z, 0, 1, 0, 0, 0, 1, red, green, blue, alpha);
		}
	}

	private static void quad(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
			float ax, float ay, float az, float bx, float by, float bz,
			int red, int green, int blue, int alpha) {
		if (matrix == null) {
			consumer.addVertex(x, y, z).setColor(red, green, blue, alpha);
			consumer.addVertex(x + ax, y + ay, z + az).setColor(red, green, blue, alpha);
			consumer.addVertex(x + ax + bx, y + ay + by, z + az + bz).setColor(red, green, blue, alpha);
			consumer.addVertex(x + bx, y + by, z + bz).setColor(red, green, blue, alpha);
			return;
		}
		consumer.addVertex(matrix, x, y, z).setColor(red, green, blue, alpha);
		consumer.addVertex(matrix, x + ax, y + ay, z + az).setColor(red, green, blue, alpha);
		consumer.addVertex(matrix, x + ax + bx, y + ay + by, z + az + bz).setColor(red, green, blue, alpha);
		consumer.addVertex(matrix, x + bx, y + by, z + bz).setColor(red, green, blue, alpha);
	}
}
