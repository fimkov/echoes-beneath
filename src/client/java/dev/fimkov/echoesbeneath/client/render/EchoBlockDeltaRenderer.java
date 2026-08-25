package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import dev.fimkov.echoesbeneath.client.wave.EchoWave;
import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class EchoBlockDeltaRenderer {
	private static final double MAX_DELTA_DISTANCE = 96.0D;
	private static final float COLOUR_SCALE = 1.0F / 255.0F;
	private static final double BLOCK_CENTRE = 0.5D;

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
		VertexConsumer consumer = context.consumers().getBuffer(RenderType.lines());

		for (int packed : deltas.trackedPositions()) {
			if (!deltas.isSolidAt(packed)) {
				continue;
			}
			double worldX = center.getX() + EchoCoordinates.unpackX(packed);
			double worldY = center.getY() + EchoCoordinates.unpackY(packed);
			double worldZ = center.getZ() + EchoCoordinates.unpackZ(packed);
			if (cameraPosition.distanceToSqr(worldX, worldY, worldZ) > MAX_DELTA_DISTANCE * MAX_DELTA_DISTANCE) {
				continue;
			}
			int colour = EchoWaveRenderer.colorFromWaves(waves, session.currentTick(), partialTick,
					worldX + BLOCK_CENTRE, worldY + BLOCK_CENTRE, worldZ + BLOCK_CENTRE);
			if ((colour >>> 24) == 0) {
				continue;
			}
			AABB box = new AABB(worldX, worldY, worldZ, worldX + 1.0D, worldY + 1.0D, worldZ + 1.0D)
					.move(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
			LevelRenderer.renderLineBox(poseStack, consumer, box,
					((colour >> 16) & 0xFF) * COLOUR_SCALE,
					((colour >> 8) & 0xFF) * COLOUR_SCALE,
					(colour & 0xFF) * COLOUR_SCALE,
					((colour >>> 24) & 0xFF) * COLOUR_SCALE);
		}
	}
}
