package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class EchoBoundaryRenderer {
	private static final int SEGMENTS = 48;
	private static final int SHELL_COLOR_OUTSIDE = 0xB4060810;
	private static final int SHELL_COLOR_INSIDE = 0x28060810;
	private static final int PULSE_COLOR = 0x60009295;
	private static final float PULSE_PERIOD_TICKS = 70.0F;
	private static final double MAX_BOUNDARY_DISTANCE = 220.0D;

	private EchoBoundaryRenderer() {
	}

	public static void render(WorldRenderContext context, ClientPlaybackSession session, boolean cameraInside) {
		if (context.consumers() == null) {
			return;
		}
		Vec3 cameraPosition = context.camera().getPosition();
		BlockPos center = session.region().center();
		if (cameraPosition.distanceToSqr(center.getX(), center.getY(), center.getZ())
				> MAX_BOUNDARY_DISTANCE * MAX_BOUNDARY_DISTANCE) {
			return;
		}
		float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
		float pulse = (float) Math.abs(Math.sin((session.currentTick() + partialTick) / PULSE_PERIOD_TICKS * Math.PI));
		PoseStack poseStack = context.matrixStack();
		poseStack.pushPose();
		poseStack.translate(center.getX() + 0.5D - cameraPosition.x,
				center.getY() + 0.5D - cameraPosition.y, center.getZ() + 0.5D - cameraPosition.z);
		Matrix4f matrix = poseStack.last().pose();
		VertexConsumer consumer = context.consumers().getBuffer(RenderType.debugQuads());
		int shellColor = cameraInside ? SHELL_COLOR_INSIDE : SHELL_COLOR_OUTSIDE;
		emitCylinder(consumer, matrix, session.region().radius(), session.region().verticalRadius(), shellColor);
		emitPulseRing(consumer, matrix, session.region().radius(), pulse);
		poseStack.popPose();
	}

	private static void emitCylinder(VertexConsumer consumer, Matrix4f matrix, int radius, int verticalRadius,
			int argb) {
		float top = verticalRadius;
		float bottom = -verticalRadius;
		for (int segment = 0; segment < SEGMENTS; segment++) {
			double angleFrom = segment * 2.0D * Math.PI / SEGMENTS;
			double angleTo = (segment + 1) * 2.0D * Math.PI / SEGMENTS;
			float x1 = (float) (Math.cos(angleFrom) * radius);
			float z1 = (float) (Math.sin(angleFrom) * radius);
			float x2 = (float) (Math.cos(angleTo) * radius);
			float z2 = (float) (Math.sin(angleTo) * radius);
			quad(consumer, matrix, x1, bottom, z1, x2, bottom, z2, x2, top, z2, x1, top, z1, argb);
		}
	}

	private static void emitPulseRing(VertexConsumer consumer, Matrix4f matrix, int radius, float pulse) {
		int alpha = Math.round(((PULSE_COLOR >>> 24) & 0xFF) * pulse);
		int color = (alpha << 24) | (PULSE_COLOR & 0x00FFFFFF);
		float bandHalfHeight = 1.0F + pulse * 2.0F;
		for (int segment = 0; segment < SEGMENTS; segment++) {
			double angleFrom = segment * 2.0D * Math.PI / SEGMENTS;
			double angleTo = (segment + 1) * 2.0D * Math.PI / SEGMENTS;
			float x1 = (float) (Math.cos(angleFrom) * radius);
			float z1 = (float) (Math.sin(angleFrom) * radius);
			float x2 = (float) (Math.cos(angleTo) * radius);
			float z2 = (float) (Math.sin(angleTo) * radius);
			quad(consumer, matrix, x1, -bandHalfHeight, z1, x2, -bandHalfHeight, z2,
					x2, bandHalfHeight, z2, x1, bandHalfHeight, z1, color);
		}
	}

	private static void quad(VertexConsumer consumer, Matrix4f matrix,
			float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float x4, float y4, float z4, int argb) {
		int alpha = (argb >>> 24) & 0xFF;
		int red = (argb >> 16) & 0xFF;
		int green = (argb >> 8) & 0xFF;
		int blue = argb & 0xFF;
		consumer.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, alpha);
		consumer.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, alpha);
		consumer.addVertex(matrix, x3, y3, z3).setColor(red, green, blue, alpha);
		consumer.addVertex(matrix, x4, y4, z4).setColor(red, green, blue, alpha);
	}
}
