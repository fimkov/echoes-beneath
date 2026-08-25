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
	private static final int MAX_HIGHLIGHT_FACES = 6000;
	private static final int SUBDIVISIONS = 4;
	private static final int GRID_POINTS = SUBDIVISIONS + 1;

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
		float[] basis = new float[9];
		float[] gridX = new float[GRID_POINTS * GRID_POINTS];
		float[] gridY = new float[GRID_POINTS * GRID_POINTS];
		float[] gridZ = new float[GRID_POINTS * GRID_POINTS];
		int[] gridColor = new int[GRID_POINTS * GRID_POINTS];
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
				EchoFaceGeometry.faceBasis(packedFace, mesh.originX(), mesh.originY(), mesh.originZ(), basis);
				if (!shadeGrid(nearby, currentTick, partialTick, center, basis, gridX, gridY, gridZ, gridColor)) {
					continue;
				}
				emitSubQuads(consumer, matrix, gridX, gridY, gridZ, gridColor);
				emitted++;
			}
		}
	}

	private static boolean shadeGrid(List<EchoWave> nearby, int currentTick, float partialTick, BlockPos center,
			float[] basis, float[] gridX, float[] gridY, float[] gridZ, int[] gridColor) {
		boolean anyVisible = false;
		for (int row = 0; row < GRID_POINTS; row++) {
			float v = (float) row / SUBDIVISIONS;
			for (int column = 0; column < GRID_POINTS; column++) {
				float u = (float) column / SUBDIVISIONS;
				int index = row * GRID_POINTS + column;
				float x = basis[0] + basis[3] * u + basis[6] * v;
				float y = basis[1] + basis[4] * u + basis[7] * v;
				float z = basis[2] + basis[5] * u + basis[8] * v;
				gridX[index] = x;
				gridY[index] = y;
				gridZ[index] = z;
				int color = colorFromWaves(nearby, currentTick, partialTick,
						center.getX() + x, center.getY() + y, center.getZ() + z);
				gridColor[index] = color;
				if ((color >>> 24) != 0) {
					anyVisible = true;
				}
			}
		}
		return anyVisible;
	}

	private static void emitSubQuads(VertexConsumer consumer, Matrix4f matrix,
			float[] gridX, float[] gridY, float[] gridZ, int[] gridColor) {
		for (int row = 0; row < SUBDIVISIONS; row++) {
			for (int column = 0; column < SUBDIVISIONS; column++) {
				int bottomLeft = row * GRID_POINTS + column;
				int bottomRight = bottomLeft + 1;
				int topRight = bottomRight + GRID_POINTS;
				int topLeft = bottomLeft + GRID_POINTS;
				if ((gridColor[bottomLeft] >>> 24) == 0 && (gridColor[bottomRight] >>> 24) == 0
						&& (gridColor[topRight] >>> 24) == 0 && (gridColor[topLeft] >>> 24) == 0) {
					continue;
				}
				emitCorner(consumer, matrix, gridX, gridY, gridZ, gridColor, bottomLeft);
				emitCorner(consumer, matrix, gridX, gridY, gridZ, gridColor, bottomRight);
				emitCorner(consumer, matrix, gridX, gridY, gridZ, gridColor, topRight);
				emitCorner(consumer, matrix, gridX, gridY, gridZ, gridColor, topLeft);
			}
		}
	}

	private static void emitCorner(VertexConsumer consumer, Matrix4f matrix,
			float[] gridX, float[] gridY, float[] gridZ, int[] gridColor, int index) {
		EchoFaceGeometry.emitVertex(consumer, matrix,
				gridX[index], gridY[index], gridZ[index], gridColor[index]);
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
			int color = EchoWavePalette.colorAt(wave.paletteIndex(), depth, wave.fadeAt(elapsed));
			int alpha = (color >>> 24) & 0xFF;
			if (alpha > bestAlpha) {
				bestAlpha = alpha;
				best = color;
			}
		}
		return bestAlpha == 0 ? 0 : best;
	}
}
