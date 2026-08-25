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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EchoWaveRenderer {
	private static final int MAX_HIGHLIGHT_FACES = 30000;
	private static final int CORNERS = EchoFaceGeometry.CORNERS_PER_FACE;

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
		emitRings(session, waves, center, partialTick, matrix, consumer);
		poseStack.popPose();
	}

	private static void emitRings(ClientPlaybackSession session, List<EchoWave> waves, BlockPos center,
			float partialTick, Matrix4f matrix, VertexConsumer consumer) {
		int currentTick = session.currentTick();
		float[] corners = new float[CORNERS * EchoFaceGeometry.COMPONENTS_PER_CORNER];
		int[] cornerColors = new int[CORNERS];
		List<EchoWave> nearby = new ArrayList<>();
		int emitted = 0;

		for (Map.Entry<Integer, EchoSectionMesh> entry : session.meshes().entrySet()) {
			EchoSectionMesh mesh = entry.getValue();
			collectNearbyWaves(mesh, waves, center, currentTick, partialTick, nearby);
			if (nearby.isEmpty()) {
				continue;
			}
			for (int packedFace : mesh.faces()) {
				if (emitted >= MAX_HIGHLIGHT_FACES) {
					return;
				}
				EchoFaceGeometry.cornerOffsets(packedFace, mesh.originX(), mesh.originY(), mesh.originZ(), corners);
				if (!shadeCorners(nearby, currentTick, partialTick, center, corners, cornerColors)) {
					continue;
				}
				EchoFaceGeometry.emitGradient(consumer, matrix, corners, cornerColors);
				emitted++;
			}
		}
	}

	private static boolean shadeCorners(List<EchoWave> nearby, int currentTick, float partialTick,
			BlockPos center, float[] corners, int[] cornerColors) {
		boolean anyVisible = false;
		for (int corner = 0; corner < CORNERS; corner++) {
			int color = colorFromWaves(nearby, currentTick, partialTick,
					center.getX() + corners[corner * EchoFaceGeometry.COMPONENTS_PER_CORNER],
					center.getY() + corners[corner * EchoFaceGeometry.COMPONENTS_PER_CORNER + 1],
					center.getZ() + corners[corner * EchoFaceGeometry.COMPONENTS_PER_CORNER + 2]);
			cornerColors[corner] = color;
			if ((color >>> 24) != 0) {
				anyVisible = true;
			}
		}
		return anyVisible;
	}

	private static void collectNearbyWaves(EchoSectionMesh mesh, List<EchoWave> waves, BlockPos center,
			int currentTick, float partialTick, List<EchoWave> destination) {
		destination.clear();
		Vec3 sectionCentre = new Vec3(
				center.getX() + mesh.originX() + 8.0D,
				center.getY() + mesh.originY() + 8.0D,
				center.getZ() + mesh.originZ() + 8.0D);
		for (EchoWave wave : waves) {
			float elapsed = wave.elapsedAt(currentTick, partialTick);
			double front = wave.radiusAt(elapsed);
			double distance = wave.origin().distanceTo(sectionCentre);
			double reach = EchoSectionMesh.CIRCUMRADIUS + wave.shellThickness();
			if (Math.abs(distance - front) <= reach) {
				destination.add(wave);
			}
		}
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
