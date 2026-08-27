package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class EchoChartAura {
	private static final int RUN_COUNT = 9;
	private static final int RUN_SIDES = 3;
	private static final float GOLDEN_TURN = 2.399963F;
	private static final float REACH_SPREAD = 0.22F;
	private static final float SHORTEST = 0.62F;
	private static final float TALLEST = 1.0F;
	private static final float RUN_WIDTH = 0.030F;
	private static final int RUN_ALPHA = 190;
	private static final float FADE_POWER = 1.5F;
	private static final float DRIFT_PER_TICK = 0.0030F;
	private static final int GAPS_PER_RUN = 2;
	private static final float GAP_WIDTH = 0.13F;
	private static final int RUN_RED = 0x1B;
	private static final int RUN_GREEN = 0xC5;
	private static final int RUN_BLUE = 0xCE;

	private static final int MOTES_PER_RUN = 7;
	private static final float COIL_NEAR = 0.042F;
	private static final float COIL_FAR = 0.085F;
	private static final float MOTE_SMALL = 0.009F;
	private static final float MOTE_LARGE = 0.019F;
	private static final float COILS_PER_RUN = 3.4F;
	private static final float CLIMB_PER_TICK = 0.0042F;
	private static final int MOTE_ALPHA = 235;
	private static final int[] WARDEN = {0x3AF0FA, 0x1BC5CE, 0x0F8A94, 0x6C4CA0};

	private static final float[] ANGLE = new float[RUN_COUNT];
	private static final float[] REACH = new float[RUN_COUNT];
	private static final float[] TOP = new float[RUN_COUNT];
	private static final float[] PHASE = new float[RUN_COUNT];

	static {
		for (int index = 0; index < RUN_COUNT; index++) {
			ANGLE[index] = index * GOLDEN_TURN + scatter(index, 11) * 0.7F;
			REACH[index] = 1.0F - REACH_SPREAD * scatter(index, 23);
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

	public static void render(PoseStack poseStack, SubmitNodeCollector submits, float age,
			boolean withMotes, boolean withRuns, Quaternionf camera, float brightness,
			float reach, float floor, float ceiling) {
		float band = Math.max(0.05F, ceiling - floor);
		submits.submitCustomGeometry(poseStack, EchoRenderTypes.ECHO_TEXELS, (pose, consumer) -> {
			if (withRuns) {
				runs(consumer, pose.pose(), age, brightness, reach, floor, band);
			}

			if (withMotes) {
				motes(pose.pose(), consumer, age, camera, brightness, reach, floor, band);
			}
		});
	}

	private static void runs(VertexConsumer consumer, Matrix4f pose, float age, float brightness,
			float reach, float floor, float band) {
		for (int index = 0; index < RUN_COUNT; index++) {
			float x = Mth.cos(ANGLE[index]) * REACH[index] * reach;
			float z = Mth.sin(ANGLE[index]) * REACH[index] * reach;
			float drift = wrap(age * DRIFT_PER_TICK + PHASE[index]);
			float cursor = 0.0F;

			for (int gap = 0; gap < GAPS_PER_RUN; gap++) {
				float mouth = wrap(drift + gap / (float) GAPS_PER_RUN);

				if (mouth <= cursor || mouth >= 1.0F) {
					continue;
				}
				run(consumer, pose, x, z, cursor, mouth, floor, TOP[index] * band, brightness);
				cursor = Math.min(1.0F, mouth + GAP_WIDTH);
			}
			run(consumer, pose, x, z, cursor, 1.0F, floor, TOP[index] * band, brightness);
		}
	}

	private static void run(VertexConsumer consumer, Matrix4f pose, float x, float z,
			float from, float to, float floor, float height, float brightness) {
		if (to - from <= 0.02F) {
			return;
		}

		float bottom = floor + from * height;
		float top = floor + to * height;
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

	private static void motes(Matrix4f base, VertexConsumer consumer, float age,
			Quaternionf camera, float brightness, float reach, float floor, float band) {
		for (int run = 0; run < RUN_COUNT; run++) {
			float runX = Mth.cos(ANGLE[run]) * REACH[run] * reach;
			float runZ = Mth.sin(ANGLE[run]) * REACH[run] * reach;

			for (int step = 0; step < MOTES_PER_RUN; step++) {
				int index = run * MOTES_PER_RUN + step;
				float climb = wrap(step / (float) MOTES_PER_RUN + scatter(index, 71) * 0.14F
						+ age * CLIMB_PER_TICK * (0.7F + scatter(index, 83) * 0.7F));
				float coil = (COIL_NEAR + (COIL_FAR - COIL_NEAR) * scatter(index, 89)) * reach * 2.4F;
				float turn = climb * COILS_PER_RUN * Mth.TWO_PI + scatter(index, 97) * Mth.TWO_PI;
				float size = MOTE_SMALL + (MOTE_LARGE - MOTE_SMALL) * scatter(index, 113);
				int colour = WARDEN[index % WARDEN.length];
				int alpha = Math.round(MOTE_ALPHA * brightness
						* (float) Math.pow(1.0F - climb, FADE_POWER));

				if (alpha <= 2) {
					continue;
				}
				Matrix4f pose = new Matrix4f(base)
						.translate(runX + Mth.cos(turn) * coil, floor + climb * TOP[run] * band,
								runZ + Mth.sin(turn) * coil)
						.rotate(camera);
				facing(consumer, pose, size, colour, alpha);
			}
		}
	}

	private static void facing(VertexConsumer consumer, Matrix4f pose, float size, int colour,
			int alpha) {
		int red = (colour >> 16) & 0xFF;
		int green = (colour >> 8) & 0xFF;
		int blue = colour & 0xFF;
		quad(consumer, pose, -size, -size, 0.0F, size, -size, 0.0F, size, size, 0.0F,
				-size, size, 0.0F, red, green, blue, alpha, alpha);
		quad(consumer, pose, -size, size, 0.0F, size, size, 0.0F, size, -size, 0.0F,
				-size, -size, 0.0F, red, green, blue, alpha, alpha);
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
