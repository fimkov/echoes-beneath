package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import dev.fimkov.echoesbeneath.client.wave.EchoWave;
import dev.fimkov.echoesbeneath.client.wave.EchoWavePalette;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;

public final class EchoWaveRenderer {
	private static final int MAX_HIGHLIGHT_FACES = 20000;
	private static final float FACE_CENTRE_OFFSET = 0.5F;
	private static final double SECTION_INFLUENCE_MARGIN = 18.0D;

	private EchoWaveRenderer() {
	}

	public static void render(WorldRenderContext context, ClientPlaybackSession session, Vec3 cameraPosition) {
		List<EchoWave> waves = session.waves().active();
		if (waves.isEmpty() || context.consumers() == null) {
			return;
		}
		float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
		BlockPos center = session.region().center();
		PoseStack poseStack = context.matrixStack();
		poseStack.pushPose();
		poseStack.translate(center.getX() - cameraPosition.x,
				center.getY() - cameraPosition.y, center.getZ() - cameraPosition.z);
		Matrix4f matrix = poseStack.last().pose();
		VertexConsumer consumer = context.consumers().getBuffer(RenderType.debugQuads());
		emitHighlightedFaces(session, waves, center, partialTick, matrix, consumer);
		poseStack.popPose();
	}

	private static void emitHighlightedFaces(ClientPlaybackSession session, List<EchoWave> waves, BlockPos center,
			float partialTick, Matrix4f matrix, VertexConsumer consumer) {
		int emitted = 0;
		for (Map.Entry<Integer, EchoSectionMesh> entry : session.meshes().entrySet()) {
			EchoSectionMesh sectionMesh = entry.getValue();
			if (!sectionInfluenced(sectionMesh, waves, center, session.currentTick(), partialTick)) {
				continue;
			}
			for (int packedFace : sectionMesh.faces()) {
				if (emitted >= MAX_HIGHLIGHT_FACES) {
					return;
				}
				double worldX = center.getX() + sectionMesh.originX()
						+ EchoFaceGeometry.localX(packedFace) + FACE_CENTRE_OFFSET;
				double worldY = center.getY() + sectionMesh.originY()
						+ EchoFaceGeometry.localY(packedFace) + FACE_CENTRE_OFFSET;
				double worldZ = center.getZ() + sectionMesh.originZ()
						+ EchoFaceGeometry.localZ(packedFace) + FACE_CENTRE_OFFSET;
				int color = colorFromWaves(waves, session.currentTick(), partialTick, worldX, worldY, worldZ);
				if (color == 0) {
					continue;
				}
				EchoFaceGeometry.emit(consumer, matrix,
						sectionMesh.originX(), sectionMesh.originY(), sectionMesh.originZ(), packedFace, color);
				emitted++;
			}
		}
	}

	private static boolean sectionInfluenced(EchoSectionMesh sectionMesh, List<EchoWave> waves, BlockPos center,
			int currentTick, float partialTick) {
		Vec3 sectionCenter = new Vec3(
				center.getX() + sectionMesh.originX() + 8.0D,
				center.getY() + sectionMesh.originY() + 8.0D,
				center.getZ() + sectionMesh.originZ() + 8.0D);
		for (EchoWave wave : waves) {
			float elapsed = wave.elapsedAt(currentTick, partialTick);
			double front = wave.radiusAt(elapsed);
			double distance = wave.origin().distanceTo(sectionCenter);
			if (Math.abs(distance - front) <= SECTION_INFLUENCE_MARGIN) {
				return true;
			}
		}
		return false;
	}

	public static int colorFromWaves(List<EchoWave> waves, int currentTick, float partialTick,
			double worldX, double worldY, double worldZ) {
		Vec3 point = new Vec3(worldX, worldY, worldZ);
		int best = 0;
		int bestAlpha = 0;
		for (EchoWave wave : waves) {
			float elapsed = wave.elapsedAt(currentTick, partialTick);
			float depth = wave.depthOf(wave.origin().distanceTo(point), elapsed);
			if (depth < 0.0F) {
				continue;
			}
			int color = EchoWavePalette.colorAt(depth, wave.fadeAt(elapsed));
			int alpha = (color >>> 24) & 0xFF;
			if (alpha > bestAlpha) {
				bestAlpha = alpha;
				best = color;
			}
		}
		return bestAlpha == 0 ? 0 : best;
	}
}
