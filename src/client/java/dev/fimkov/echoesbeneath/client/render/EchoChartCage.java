package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class EchoChartCage {
	private static final float REACH = 0.34F;
	private static final float HEIGHT = 0.94F;
	private static final float THICKNESS = 0.011F;
	private static final int RED = 0x29;
	private static final int GREEN = 0xDF;
	private static final int BLUE = 0xEB;
	private static final int BRIGHTEST = 215;
	private static final float FADE_POWER = 1.6F;
	private static final float DRIFT_PER_TICK = 0.0035F;
	private static final float[][] SEGMENTS = {
			{0.00F, 0.34F}, {0.41F, 0.60F}, {0.66F, 0.79F}, {0.85F, 0.93F}};
	private static final float[][] POSTS = {
			{-1.0F, -1.0F}, {1.0F, -1.0F}, {1.0F, 1.0F}, {-1.0F, 1.0F},
			{0.0F, -1.0F}, {1.0F, 0.0F}, {0.0F, 1.0F}, {-1.0F, 0.0F}};

	private EchoChartCage() {
	}

	public static void render(PoseStack poseStack, MultiBufferSource consumers, float age) {
		VertexConsumer consumer = consumers.getBuffer(EchoRenderTypes.ECHO_TEXELS);
		Matrix4f pose = poseStack.last().pose();
		float drift = (age * DRIFT_PER_TICK) % 1.0F;
		for (float[] post : POSTS) {
			float x = post[0] * REACH;
			float z = post[1] * REACH;
			for (float[] segment : SEGMENTS) {
				float low = wrap(segment[0] + drift);
				float high = low + (segment[1] - segment[0]);
				if (high > 1.0F) {
					column(consumer, pose, x, z, low, 1.0F);
					column(consumer, pose, x, z, 0.0F, high - 1.0F);
				} else {
					column(consumer, pose, x, z, low, high);
				}
			}
		}
	}

	private static float wrap(float value) {
		return value - (float) Math.floor(value);
	}

	private static void column(VertexConsumer consumer, Matrix4f pose, float x, float z,
			float from, float to) {
		if (to - from <= 0.001F) {
			return;
		}
		float bottom = from * HEIGHT;
		float top = to * HEIGHT;
		int lowAlpha = alphaAt(from);
		int highAlpha = alphaAt(to);
		side(consumer, pose, x - THICKNESS, z - THICKNESS, x + THICKNESS, z - THICKNESS,
				bottom, top, lowAlpha, highAlpha);
		side(consumer, pose, x + THICKNESS, z - THICKNESS, x + THICKNESS, z + THICKNESS,
				bottom, top, lowAlpha, highAlpha);
		side(consumer, pose, x + THICKNESS, z + THICKNESS, x - THICKNESS, z + THICKNESS,
				bottom, top, lowAlpha, highAlpha);
		side(consumer, pose, x - THICKNESS, z + THICKNESS, x - THICKNESS, z - THICKNESS,
				bottom, top, lowAlpha, highAlpha);
	}

	private static int alphaAt(float fraction) {
		return Math.round(BRIGHTEST * (float) Math.pow(1.0F - Mth.clamp(fraction, 0.0F, 1.0F),
				FADE_POWER));
	}

	private static void side(VertexConsumer consumer, Matrix4f pose, float x0, float z0,
			float x1, float z1, float bottom, float top, int lowAlpha, int highAlpha) {
		consumer.addVertex(pose, x0, bottom, z0).setColor(RED, GREEN, BLUE, lowAlpha);
		consumer.addVertex(pose, x1, bottom, z1).setColor(RED, GREEN, BLUE, lowAlpha);
		consumer.addVertex(pose, x1, top, z1).setColor(RED, GREEN, BLUE, highAlpha);
		consumer.addVertex(pose, x0, top, z0).setColor(RED, GREEN, BLUE, highAlpha);
	}
}
