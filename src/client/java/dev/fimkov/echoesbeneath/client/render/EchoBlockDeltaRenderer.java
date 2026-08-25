package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import dev.fimkov.echoesbeneath.client.wave.EchoWave;
import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class EchoBlockDeltaRenderer {
	private static final double MAX_DELTA_DISTANCE = 96.0D;
	private static final int MAX_DELTA_TEXELS = 8000;

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
		int currentTick = session.currentTick();
		int animationStep = currentTick / EchoTexelGrid.TICKS_PER_ANIMATION_STEP;
		float texel = EchoTexelGrid.texelSize();

		PoseStack poseStack = context.matrixStack();
		poseStack.pushPose();
		poseStack.translate(center.getX() - cameraPosition.x,
				center.getY() - cameraPosition.y, center.getZ() - cameraPosition.z);
		VertexConsumer consumer = context.consumers().getBuffer(EchoRenderTypes.ECHO_TEXELS);

		float[] basis = new float[9];
		float[] sample = new float[EchoWaveRenderer.SAMPLE_SLOTS];
		List<EchoWave> facing = new ArrayList<>();
		int emitted = 0;

		for (int packed : deltas.trackedPositions()) {
			if (emitted >= MAX_DELTA_TEXELS) {
				break;
			}
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
			for (Direction direction : Direction.values()) {
				if (deltas.isSolidAt(relativeX + direction.getStepX(),
						relativeY + direction.getStepY(), relativeZ + direction.getStepZ())) {
					continue;
				}
				int packedFace = EchoFaceGeometry.pack(0, 0, 0, direction);
				EchoFaceGeometry.faceBasis(packedFace, relativeX, relativeY, relativeZ, basis);
				EchoWaveRenderer.collectFacingWaves(waves, packedFace, basis, center, facing);
				if (facing.isEmpty()) {
					continue;
				}
				emitted += EchoWaveRenderer.lightFace(consumer, poseStack.last().pose(), facing, center,
						basis, texel, currentTick, partialTick, animationStep, sample,
						MAX_DELTA_TEXELS - emitted);
			}
		}
		poseStack.popPose();
	}
}
