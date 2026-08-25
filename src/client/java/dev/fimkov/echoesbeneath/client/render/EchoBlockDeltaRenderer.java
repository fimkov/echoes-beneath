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
	private static final int OPAQUE_ALPHA = 0xFF000000;
	private static final double MAX_DELTA_DISTANCE = 96.0D;
	private static final float FACE_CENTRE_OFFSET = 0.5F;

	private EchoBlockDeltaRenderer() {
	}

	public static void render(WorldRenderContext context, ClientPlaybackSession session, Vec3 cameraPosition) {
		if (context.consumers() == null) {
			return;
		}
		EchoBlockDeltaState deltas = session.blockDeltas();
		if (deltas.trackedPositions().isEmpty()) {
			return;
		}
		BlockPos center = session.region().center();
		float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
		List<EchoWave> waves = session.waves().active();
		PoseStack poseStack = context.matrixStack();
		poseStack.pushPose();
		poseStack.translate(center.getX() - cameraPosition.x,
				center.getY() - cameraPosition.y, center.getZ() - cameraPosition.z);
		Matrix4f matrix = poseStack.last().pose();
		VertexConsumer consumer = context.consumers().getBuffer(RenderType.debugQuads());
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
					relativeX, relativeY, relativeZ);
		}
		poseStack.popPose();
	}

	private static void emitCube(VertexConsumer consumer, Matrix4f matrix, EchoBlockDeltaState deltas,
			List<EchoWave> waves, int currentTick, float partialTick, BlockPos center,
			int relativeX, int relativeY, int relativeZ) {
		for (Direction direction : Direction.values()) {
			if (deltas.isSolidAt(relativeX + direction.getStepX(),
					relativeY + direction.getStepY(), relativeZ + direction.getStepZ())) {
				continue;
			}
			int packedFace = EchoFaceGeometry.pack(0, 0, 0, direction);
			int waveColor = EchoWaveRenderer.colorFromWaves(waves, currentTick, partialTick,
					center.getX() + relativeX + FACE_CENTRE_OFFSET,
					center.getY() + relativeY + FACE_CENTRE_OFFSET,
					center.getZ() + relativeZ + FACE_CENTRE_OFFSET);
			int color = waveColor != 0 ? waveColor : OPAQUE_ALPHA | EchoFaceGeometry.shadeColor(direction);
			EchoFaceGeometry.emit(consumer, matrix, relativeX, relativeY, relativeZ, packedFace, color);
		}
	}
}
