package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import dev.fimkov.echoesbeneath.client.wave.EchoWave;
import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public final class EchoBlockDeltaRenderer {
	private static final double MAX_DELTA_DISTANCE = 96.0D;
	private static final int CORNERS = EchoFaceGeometry.CORNERS_PER_FACE;
	private static final float DELTA_TEXTURE_ROW = 0.5F;

	private EchoBlockDeltaRenderer() {
	}

	public static void render(WorldRenderContext context, ClientPlaybackSession session, Vec3 cameraPosition) {
		if (context.consumers() == null) {
			return;
		}
		EchoBlockDeltaState deltas = session.blockDeltas();
		List<EchoWave> waves = session.waves().active();
		if (deltas.trackedPositions().isEmpty() || waves.isEmpty()) {
			return;
		}
		BlockPos center = session.region().center();
		float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
		PoseStack poseStack = context.matrixStack();
		poseStack.pushPose();
		poseStack.translate(center.getX() - cameraPosition.x,
				center.getY() - cameraPosition.y, center.getZ() - cameraPosition.z);
		Matrix4f matrix = poseStack.last().pose();
		VertexConsumer consumer = context.consumers().getBuffer(EchoRenderTypes.ECHO_WAVE);
		float[] corners = new float[CORNERS * EchoFaceGeometry.COMPONENTS_PER_CORNER];
		int[] cornerColors = new int[CORNERS];
		float[] cornerDepths = new float[CORNERS];

		for (int packed : deltas.trackedPositions()) {
			if (!deltas.isSolidAt(packed)) {
				continue;
			}
			int relativeX = EchoCoordinates.unpackX(packed);
			int relativeY = EchoCoordinates.unpackY(packed);
			int relativeZ = EchoCoordinates.unpackZ(packed);
			double worldX = center.getX() + relativeX;
			double worldY = center.getY() + relativeY;
			double worldZ = center.getZ() + relativeZ;
			if (cameraPosition.distanceToSqr(worldX, worldY, worldZ) > MAX_DELTA_DISTANCE * MAX_DELTA_DISTANCE) {
				continue;
			}
			emitCube(consumer, matrix, deltas, waves, session.currentTick(), partialTick, center,
					relativeX, relativeY, relativeZ, corners, cornerColors, cornerDepths);
		}
		poseStack.popPose();
	}

	private static void emitCube(VertexConsumer consumer, Matrix4f matrix, EchoBlockDeltaState deltas,
			List<EchoWave> waves, int currentTick, float partialTick, BlockPos center,
			int relativeX, int relativeY, int relativeZ, float[] corners, int[] cornerColors,
			float[] cornerDepths) {
		for (Direction direction : Direction.values()) {
			if (deltas.isSolidAt(relativeX + direction.getStepX(),
					relativeY + direction.getStepY(), relativeZ + direction.getStepZ())) {
				continue;
			}
			int packedFace = EchoFaceGeometry.pack(0, 0, 0, direction);
			EchoFaceGeometry.cornerOffsets(packedFace, relativeX, relativeY, relativeZ, corners);
			boolean visible = false;
			float[] depthOut = new float[1];
			for (int corner = 0; corner < CORNERS; corner++) {
				int color = EchoWaveRenderer.shadePoint(waves, currentTick, partialTick,
						center.getX() + corners[corner * EchoFaceGeometry.COMPONENTS_PER_CORNER],
						center.getY() + corners[corner * EchoFaceGeometry.COMPONENTS_PER_CORNER + 1],
						center.getZ() + corners[corner * EchoFaceGeometry.COMPONENTS_PER_CORNER + 2],
						depthOut);
				cornerColors[corner] = color;
				cornerDepths[corner] = depthOut[0];
				if ((color >>> 24) != 0) {
					visible = true;
				}
			}
			if (visible) {
				for (int corner = 0; corner < CORNERS; corner++) {
					EchoFaceGeometry.emitVertex(consumer, matrix,
							corners[corner * EchoFaceGeometry.COMPONENTS_PER_CORNER],
							corners[corner * EchoFaceGeometry.COMPONENTS_PER_CORNER + 1],
							corners[corner * EchoFaceGeometry.COMPONENTS_PER_CORNER + 2],
							cornerDepths[corner], DELTA_TEXTURE_ROW, cornerColors[corner]);
				}
			}
		}
	}
}
