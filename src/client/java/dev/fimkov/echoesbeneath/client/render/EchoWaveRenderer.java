package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import dev.fimkov.echoesbeneath.client.wave.EchoWave;
import dev.fimkov.echoesbeneath.client.wave.EchoWavePalette;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EchoWaveRenderer {
	private static final int MAX_HIGHLIGHT_FACES = 6000;
	private static final int SUBDIVISIONS = 4;
	private static final int GRID_POINTS = SUBDIVISIONS + 1;
	private static final int TEXTURE_ROWS = 8;

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
		VertexConsumer consumer = context.consumers().getBuffer(EchoRenderTypes.ECHO_WAVE);
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
		float[] gridU = new float[GRID_POINTS * GRID_POINTS];
		int[] gridColor = new int[GRID_POINTS * GRID_POINTS];
		float[] depthOut = new float[1];
		List<EchoWave> nearby = new ArrayList<>();
		List<EchoWave> facing = new ArrayList<>();
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
				collectFacingWaves(nearby, packedFace, basis, center, facing);
				if (facing.isEmpty()) {
					continue;
				}
				if (!shadeGrid(facing, currentTick, partialTick, center, basis,
						gridX, gridY, gridZ, gridU, gridColor, depthOut)) {
					continue;
				}
				emitSubQuads(consumer, matrix, gridX, gridY, gridZ, gridU, gridColor,
						textureRowFor(packedFace, mesh));
				emitted++;
			}
		}
	}

	private static float textureRowFor(int packedFace, EchoSectionMesh mesh) {
		int hash = (mesh.originX() + EchoFaceGeometry.localX(packedFace)) * 31
				+ (mesh.originY() + EchoFaceGeometry.localY(packedFace)) * 17
				+ (mesh.originZ() + EchoFaceGeometry.localZ(packedFace)) * 13;
		return (Math.floorMod(hash, TEXTURE_ROWS) + 0.5F) / TEXTURE_ROWS;
	}

	private static boolean shadeGrid(List<EchoWave> nearby, int currentTick, float partialTick, BlockPos center,
			float[] basis, float[] gridX, float[] gridY, float[] gridZ, float[] gridU, int[] gridColor,
			float[] depthOut) {
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
				int color = shadePoint(nearby, currentTick, partialTick,
						center.getX() + x, center.getY() + y, center.getZ() + z, depthOut);
				gridColor[index] = color;
				gridU[index] = depthOut[0];
				if ((color >>> 24) != 0) {
					anyVisible = true;
				}
			}
		}
		return anyVisible;
	}

	private static void emitSubQuads(VertexConsumer consumer, Matrix4f matrix,
			float[] gridX, float[] gridY, float[] gridZ, float[] gridU, int[] gridColor, float textureRow) {
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
				emitCorner(consumer, matrix, gridX, gridY, gridZ, gridU, gridColor, bottomLeft, textureRow);
				emitCorner(consumer, matrix, gridX, gridY, gridZ, gridU, gridColor, bottomRight, textureRow);
				emitCorner(consumer, matrix, gridX, gridY, gridZ, gridU, gridColor, topRight, textureRow);
				emitCorner(consumer, matrix, gridX, gridY, gridZ, gridU, gridColor, topLeft, textureRow);
			}
		}
	}

	private static void emitCorner(VertexConsumer consumer, Matrix4f matrix,
			float[] gridX, float[] gridY, float[] gridZ, float[] gridU, int[] gridColor,
			int index, float textureRow) {
		EchoFaceGeometry.emitVertex(consumer, matrix,
				gridX[index], gridY[index], gridZ[index], gridU[index], textureRow, gridColor[index]);
	}

	private static void collectFacingWaves(List<EchoWave> nearby, int packedFace, float[] basis,
			BlockPos center, List<EchoWave> destination) {
		destination.clear();
		Direction direction = EchoFaceGeometry.direction(packedFace);
		double faceCentreX = center.getX() + basis[0] + (basis[3] + basis[6]) * 0.5D;
		double faceCentreY = center.getY() + basis[1] + (basis[4] + basis[7]) * 0.5D;
		double faceCentreZ = center.getZ() + basis[2] + (basis[5] + basis[8]) * 0.5D;
		for (EchoWave wave : nearby) {
			double toOriginX = wave.origin().x - faceCentreX;
			double toOriginY = wave.origin().y - faceCentreY;
			double toOriginZ = wave.origin().z - faceCentreZ;
			double facing = toOriginX * direction.getStepX()
					+ toOriginY * direction.getStepY()
					+ toOriginZ * direction.getStepZ();
			if (facing > 0.0D) {
				destination.add(wave);
			}
		}
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

	public static int shadePoint(List<EchoWave> waves, int currentTick, float partialTick,
			double worldX, double worldY, double worldZ, float[] depthOut) {
		Vec3 point = new Vec3(worldX, worldY, worldZ);
		int best = 0;
		int bestAlpha = 0;
		float bestDepth = 0.0F;
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
				bestDepth = depth;
			}
		}
		depthOut[0] = bestDepth;
		return bestAlpha == 0 ? 0 : best;
	}

	public static int colorFromWaves(List<EchoWave> waves, int currentTick, float partialTick,
			double worldX, double worldY, double worldZ) {
		return shadePoint(waves, currentTick, partialTick, worldX, worldY, worldZ, new float[1]);
	}
}
