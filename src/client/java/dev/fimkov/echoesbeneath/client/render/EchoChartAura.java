package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class EchoChartAura {
	private static final int RUN_COUNT = 9;
	private static final int RUN_SIDES = 3;
	private static final float GOLDEN_TURN = 2.399963F;
	private static final float NEAR_REACH = 0.24F;
	private static final float FAR_REACH = 0.44F;
	private static final float SHORTEST = 0.52F;
	private static final float TALLEST = 0.98F;
	private static final float RUN_WIDTH = 0.030F;
	private static final int RUN_ALPHA = 190;
	private static final float FADE_POWER = 1.5F;
	private static final float DRIFT_PER_TICK = 0.0030F;
	private static final int GAPS_PER_RUN = 2;
	private static final float GAP_WIDTH = 0.13F;
	private static final int RUN_RED = 0x1B;
	private static final int RUN_GREEN = 0xC5;
	private static final int RUN_BLUE = 0xCE;

	private static final int MOTE_COUNT = 15;
	private static final float MOTE_NEAR = 0.33F;
	private static final float MOTE_FAR = 0.54F;
	private static final float MOTE_SMALL = 0.026F;
	private static final float MOTE_LARGE = 0.062F;
	private static final float MOTE_TURN_PER_TICK = 0.9F;
	private static final float MOTE_LIFT = 0.94F;
	private static final float MOTE_SWAY = 0.055F;
	private static final float SWAY_PER_TICK = 0.031F;
	private static final int MOTE_ALPHA = 225;
	private static final int[] WARDEN = {0x3AF0FA, 0x1BC5CE, 0x0F8A94, 0x6C4CA0};

	private static final float[] ANGLE = new float[RUN_COUNT];
	private static final float[] REACH = new float[RUN_COUNT];
	private static final float[] TOP = new float[RUN_COUNT];
	private static final float[] PHASE = new float[RUN_COUNT];

	static {
		for (int index = 0; index < RUN_COUNT; index++) {
			ANGLE[index] = index * GOLDEN_TURN + scatter(index, 11) * 0.7F;
			REACH[index] = NEAR_REACH + (FAR_REACH - NEAR_REACH) * scatter(index, 23);
			TOP[index] = SHORTEST + (TALLEST - SHORTEST) * scatter(index, 37);
			PHASE[index] = scatter(index, 53);
		}
	}

	private EchoChartAura() {
	}

	private static float scatter(int index, int salt) {
		int mixed = (index * 0x9E3779B1) ^ (salt * 0x85EBCA6B);
		mixed ^= mixed >>> 15;
		mixed *= 0xC2B2AE35;
		mixed ^= mixed >>> 13;
		return (mixed >>> 8 & 0xFFFF) / 65535.0F;
	}

	private static float wrap(float value) {
		return value - (float) Math.floor(value);
	}

	public static void render(PoseStack poseStack, MultiBufferSource consumers, float age,
			Quaternionf camera, float brightness) {
		VertexConsumer consumer = consumers.getBuffer(EchoRenderTypes.ECHO_TEXELS);
		runs(consumer, poseStack.last().pose(), age, brightness);
		motes(poseStack, consumer, age, camera, brightness);
	}

	private static void runs(VertexConsumer consumer, Matrix4f pose, float age, float brightness) {
		for (int index = 0; index < RUN_COUNT; index++) {
			float x = Mth.cos(ANGLE[index]) * REACH[index];
			float z = Mth.sin(ANGLE[index]) * REACH[index];
			float drift = wrap(age * DRIFT_PER_TICK + PHASE[index]);
			float cursor = 0.0F;
			for (int gap = 0; gap < GAPS_PER_RUN; gap++) {
				float mouth = wrap(drift + gap / (float) GAPS_PER_RUN);
				if (mouth <= cursor || mouth >= 1.0F) {
					continue;
				}
				run(consumer, pose, x, z, cursor, mouth, TOP[index], brightness);
				cursor = Math.min(1.0F, mouth + GAP_WIDTH);
			}
			run(consumer, pose, x, z, cursor, 1.0F, TOP[index], brightness);
		}
	}

	private static void run(VertexConsumer consumer, Matrix4f pose, float x, float z,
			float from, float to, float height, float brightness) {
		if (to - from <= 0.02F) {
			return;
		}
		float bottom = from * height;
		float top = to * height;
		int lowAlpha = Math.round(alphaAt(from) * brightness);
		int highAlpha = Math.round(alphaAt(to) * brightness);
		for (int side = 0; side < RUN_SIDES; side++) {
			float first = side * Mth.TWO_PI / RUN_SIDES;
			float second = (side + 1) * Mth.TWO_PI / RUN_SIDES;
			quad(consumer, pose,
					x + Mth.cos(first) * RUN_WIDTH, bottom, z + Mth.sin(first) * RUN_WIDTH,
					x + Mth.cos(second) * RUN_WIDTH, bottom, z + Mth.sin(second) * RUN_WIDTH,
					x + Mth.cos(second) * RUN_WIDTH, top, z + Mth.sin(second) * RUN_WIDTH,
					x + Mth.cos(first) * RUN_WIDTH, top, z + Mth.sin(first) * RUN_WIDTH,
					RUN_RED, RUN_GREEN, RUN_BLUE, lowAlpha, highAlpha);
		}
	}

	private static int alphaAt(float fraction) {
		return Math.round(RUN_ALPHA * (float) Math.pow(1.0F - Mth.clamp(fraction, 0.0F, 1.0F),
				FADE_POWER));
	}

	private static void motes(PoseStack poseStack, VertexConsumer consumer, float age,
			Quaternionf camera, float brightness) {
		for (int index = 0; index < MOTE_COUNT; index++) {
			float reach = MOTE_NEAR + (MOTE_FAR - MOTE_NEAR) * scatter(index, 71);
			float speed = 0.55F + scatter(index, 83) * 0.9F;
			float turn = (index * GOLDEN_TURN
					+ age * MOTE_TURN_PER_TICK * speed * Mth.DEG_TO_RAD) % Mth.TWO_PI;
			float lift = MOTE_LIFT * scatter(index, 97)
					+ Mth.sin((age * SWAY_PER_TICK + scatter(index, 101) * Mth.TWO_PI)) * MOTE_SWAY;
			float size = MOTE_SMALL + (MOTE_LARGE - MOTE_SMALL) * scatter(index, 113);
			int colour = WARDEN[index % WARDEN.length];
			int alpha = Math.round(MOTE_ALPHA * brightness
					* (0.45F + 0.55F * (Mth.sin(age * 0.045F + index) * 0.5F + 0.5F)));
			poseStack.pushPose();
			poseStack.translate(Mth.cos(turn) * reach, lift, Mth.sin(turn) * reach);
			poseStack.mulPose(camera);
			Matrix4f pose = poseStack.last().pose();
			quad(consumer, pose,
					-size, -size, 0.0F, size, -size, 0.0F, size, size, 0.0F, -size, size, 0.0F,
					(colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, alpha, alpha);
			quad(consumer, pose,
					-size, size, 0.0F, size, size, 0.0F, size, -size, 0.0F, -size, -size, 0.0F,
					(colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, alpha, alpha);
			poseStack.popPose();
		}
	}

	private static void quad(VertexConsumer consumer, Matrix4f pose,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3,
			int red, int green, int blue, int lowAlpha, int highAlpha) {
		consumer.addVertex(pose, x0, y0, z0).setColor(red, green, blue, lowAlpha);
		consumer.addVertex(pose, x1, y1, z1).setColor(red, green, blue, lowAlpha);
		consumer.addVertex(pose, x2, y2, z2).setColor(red, green, blue, highAlpha);
		consumer.addVertex(pose, x3, y3, z3).setColor(red, green, blue, highAlpha);
	}
}
