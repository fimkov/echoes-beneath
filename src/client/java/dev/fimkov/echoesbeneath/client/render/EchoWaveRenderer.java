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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EchoWaveRenderer {
	private static final int MAX_OUTLINE_EDGES = 24000;
	private static final float COLOUR_SCALE = 1.0F / 255.0F;

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
		VertexConsumer consumer = context.consumers().getBuffer(RenderType.lines());
		traceOutlines(session, waves, center, partialTick, poseStack, consumer);
		poseStack.popPose();
	}

	private static void traceOutlines(ClientPlaybackSession session, List<EchoWave> waves, BlockPos center,
			float partialTick, PoseStack poseStack, VertexConsumer consumer) {
		int currentTick = session.currentTick();
		float[] basis = new float[9];
		float[] endpoints = new float[6];
		List<EchoWave> nearby = new ArrayList<>();
		int emitted = 0;

		for (Map.Entry<Integer, EchoSectionMesh> entry : session.meshes().entrySet()) {
			EchoSectionMesh mesh = entry.getValue();
			collectNearbyWaves(mesh, waves, center, currentTick, partialTick, nearby);
			if (nearby.isEmpty()) {
				continue;
			}
			for (int packedFace : mesh.faces()) {
				int edgeMask = EchoFaceGeometry.edgeMask(packedFace);
				if (edgeMask == 0) {
					continue;
				}
				if (emitted >= MAX_OUTLINE_EDGES) {
					return;
				}
				EchoFaceGeometry.faceBasis(packedFace, mesh.originX(), mesh.originY(), mesh.originZ(), basis);
				float faceCentreX = basis[0] + (basis[3] + basis[6]) * 0.5F;
				float faceCentreY = basis[1] + (basis[4] + basis[7]) * 0.5F;
				float faceCentreZ = basis[2] + (basis[5] + basis[8]) * 0.5F;
				int colour = colorFromWaves(nearby, currentTick, partialTick,
						center.getX() + faceCentreX, center.getY() + faceCentreY, center.getZ() + faceCentreZ);
				if ((colour >>> 24) == 0) {
					continue;
				}
				for (int edge = 0; edge < EchoFaceGeometry.EDGES_PER_FACE; edge++) {
					if ((edgeMask & (1 << edge)) == 0) {
						continue;
					}
					EchoFaceGeometry.edgeEndpoints(packedFace, mesh.originX(), mesh.originY(), mesh.originZ(),
							edge, basis, endpoints);
					emitLine(consumer, poseStack, endpoints, colour);
					emitted++;
				}
			}
		}
	}

	private static void emitLine(VertexConsumer consumer, PoseStack poseStack, float[] endpoints, int argb) {
		float red = ((argb >> 16) & 0xFF) * COLOUR_SCALE;
		float green = ((argb >> 8) & 0xFF) * COLOUR_SCALE;
		float blue = (argb & 0xFF) * COLOUR_SCALE;
		float alpha = ((argb >>> 24) & 0xFF) * COLOUR_SCALE;
		float normalX = endpoints[3] - endpoints[0];
		float normalY = endpoints[4] - endpoints[1];
		float normalZ = endpoints[5] - endpoints[2];
		float length = Math.max(1.0E-4F, (float) Math.sqrt(
				normalX * normalX + normalY * normalY + normalZ * normalZ));
		consumer.addVertex(poseStack.last().pose(), endpoints[0], endpoints[1], endpoints[2])
				.setColor(red, green, blue, alpha)
				.setNormal(poseStack.last(), normalX / length, normalY / length, normalZ / length);
		consumer.addVertex(poseStack.last().pose(), endpoints[3], endpoints[4], endpoints[5])
				.setColor(red, green, blue, alpha)
				.setNormal(poseStack.last(), normalX / length, normalY / length, normalZ / length);
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
			int colour = EchoWavePalette.colorAt(wave.paletteIndex(), depth, wave.fadeAt(elapsed));
			int alpha = (colour >>> 24) & 0xFF;
			if (alpha > bestAlpha) {
				bestAlpha = alpha;
				best = colour;
			}
		}
		return bestAlpha == 0 ? 0 : best;
	}
}
