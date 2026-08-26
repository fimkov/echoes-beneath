package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class EchoChartCage {
	private static final int POST_COUNT = 13;
	private static final int SIDES = 3;
	private static final float GOLDEN_TURN = 2.399963F;
	private static final float NEAR_REACH = 0.17F;
	private static final float FAR_REACH = 0.38F;
	private static final float SHORTEST = 0.44F;
	private static final float TALLEST = 0.92F;
	private static final float THICKNESS = 0.013F;
	private static final int RED = 0x29;
	private static final int GREEN = 0xDF;
	private static final int BLUE = 0xEB;
	private static final int BRIGHTEST = 205;
	private static final float FADE_POWER = 1.7F;
	private static final float DRIFT_PER_TICK = 0.0032F;
	private static final int GAPS_PER_POST = 2;
	private static final float GAP_WIDTH = 0.11F;

	private static final float[] ANGLE = new float[POST_COUNT];
	private static final float[] REACH = new float[POST_COUNT];
	private static final float[] TOP = new float[POST_COUNT];
	private static final float[] PHASE = new float[POST_COUNT];

	static {
		for (int index = 0; index < POST_COUNT; index++) {
			ANGLE[index] = index * GOLDEN_TURN + scatter(index, 11) * 0.6F;
			REACH[index] = NEAR_REACH + (FAR_REACH - NEAR_REACH) * scatter(index, 23);
			TOP[index] = SHORTEST + (TALLEST - SHORTEST) * scatter(index, 37);
			PHASE[index] = scatter(index, 53);
		}
	}

	private EchoChartCage() {
	}

	private static float scatter(int index, int salt) {
		int mixed = (index * 0x9E3779B1) ^ (salt * 0x85EBCA6B);
		mixed ^= mixed >>> 15;
		mixed *= 0xC2B2AE35;
		mixed ^= mixed >>> 13;
		return (mixed >>> 8 & 0xFFFF) / 65535.0F;
	}

	public static void render(PoseStack poseStack, MultiBufferSource consumers, float age) {
		VertexConsumer consumer = consumers.getBuffer(EchoRenderTypes.ECHO_TEXELS);
		Matrix4f pose = poseStack.last().pose();
		for (int index = 0; index < POST_COUNT; index++) {
			float x = Mth.cos(ANGLE[index]) * REACH[index];
			float z = Mth.sin(ANGLE[index]) * REACH[index];
			float drift = (age * DRIFT_PER_TICK + PHASE[index]) % 1.0F;
			float cursor = 0.0F;
			for (int gap = 0; gap <= GAPS_PER_POST; gap++) {
				float mouth = wrap(drift + gap / (float) (GAPS_PER_POST + 1));
				float end = Math.min(1.0F, mouth);
				if (end > cursor) {
					post(consumer, pose, x, z, cursor, end, TOP[index]);
				}
				cursor = Math.min(1.0F, mouth + GAP_WIDTH);
			}
			if (cursor < 1.0F) {
				post(consumer, pose, x, z, cursor, 1.0F, TOP[index]);
			}
		}
	}

	private static float wrap(float value) {
		return value - (float) Math.floor(value);
	}

	private static void post(VertexConsumer consumer, Matrix4f pose, float x, float z,
			float from, float to, float height) {
		if (to - from <= 0.02F) {
			return;
		}
		float bottom = from * height;
		float top = to * height;
		int lowAlpha = alphaAt(from);
		int highAlpha = alphaAt(to);
		for (int side = 0; side < SIDES; side++) {
			float first = side * Mth.TWO_PI / SIDES;
			float second = (side + 1) * Mth.TWO_PI / SIDES;
			face(consumer, pose,
					x + Mth.cos(first) * THICKNESS, z + Mth.sin(first) * THICKNESS,
					x + Mth.cos(second) * THICKNESS, z + Mth.sin(second) * THICKNESS,
					bottom, top, lowAlpha, highAlpha);
		}
	}

	private static int alphaAt(float fraction) {
		return Math.round(BRIGHTEST * (float) Math.pow(1.0F - Mth.clamp(fraction, 0.0F, 1.0F),
				FADE_POWER));
	}

	private static void face(VertexConsumer consumer, Matrix4f pose, float x0, float z0,
			float x1, float z1, float bottom, float top, int lowAlpha, int highAlpha) {
		consumer.addVertex(pose, x0, bottom, z0).setColor(RED, GREEN, BLUE, lowAlpha);
		consumer.addVertex(pose, x1, bottom, z1).setColor(RED, GREEN, BLUE, lowAlpha);
		consumer.addVertex(pose, x1, top, z1).setColor(RED, GREEN, BLUE, highAlpha);
		consumer.addVertex(pose, x0, top, z0).setColor(RED, GREEN, BLUE, highAlpha);
	}
}
