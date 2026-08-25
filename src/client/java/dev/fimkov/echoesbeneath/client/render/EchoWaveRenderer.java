package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.fimkov.echoesbeneath.client.config.EchoClientConfig;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import dev.fimkov.echoesbeneath.client.wave.EchoWave;
import dev.fimkov.echoesbeneath.client.wave.EchoWavePalette;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EchoWaveRenderer {

	private static final double FACE_REACH = 0.75D;
	public static final int SAMPLE_SLOTS = 3;
	private static final int SAMPLE_DEPTH = 0;
	private static final int SAMPLE_FADE = 1;
	private static final int SAMPLE_PALETTE = 2;
	private static final int SAMPLE_SIZE = 3;

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
		VertexConsumer consumer = context.consumers().getBuffer(EchoRenderTypes.ECHO_TEXELS);
		lightTexels(session, waves, center, partialTick, poseStack.last().pose(), consumer);
		poseStack.popPose();
	}

	private static void lightTexels(ClientPlaybackSession session, List<EchoWave> waves, BlockPos center,
			float partialTick, Matrix4f matrix, VertexConsumer consumer) {
		int currentTick = session.currentTick();
		int animationStep = currentTick / EchoTexelGrid.TICKS_PER_ANIMATION_STEP;
		float texel = EchoTexelGrid.texelSize();
		float[] basis = new float[9];
		float[] depthOut = new float[SAMPLE_SIZE];
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
				if (emitted >= EchoClientConfig.values().waveTexelBudget()) {
					return;
				}
				EchoFaceGeometry.faceBasis(packedFace, mesh.originX(), mesh.originY(), mesh.originZ(), basis);
				collectFacingWaves(nearby, packedFace, basis, center, facing, currentTick, partialTick);
				if (facing.isEmpty()) {
					continue;
				}
				emitted += lightFace(consumer, matrix, facing, center, basis, texel,
						currentTick, partialTick, animationStep, depthOut, EchoClientConfig.values().waveTexelBudget() - emitted);
			}
		}
	}

	public static int lightFace(VertexConsumer consumer, Matrix4f matrix, List<EchoWave> waves, BlockPos center,
			float[] basis, float texel, int currentTick, float partialTick, int animationStep,
			float[] depthOut, int budget) {
		int emitted = 0;
		float presence = EchoDarkness.transitionProgress();
		if (presence <= 0.0F) {
			return 0;
		}
		for (int row = 0; row < EchoTexelGrid.texelsPerBlock() && emitted < budget; row++) {
			for (int column = 0; column < EchoTexelGrid.texelsPerBlock() && emitted < budget; column++) {
				float u = (column + 0.5F) * texel;
				float v = (row + 0.5F) * texel;
				float localX = basis[0] + basis[3] * u + basis[6] * v;
				float localY = basis[1] + basis[4] * u + basis[7] * v;
				float localZ = basis[2] + basis[5] * u + basis[8] * v;
				int body = shadePoint(waves, currentTick, partialTick,
						center.getX() + localX, center.getY() + localY, center.getZ() + localZ, depthOut);
				if ((body >>> 24) == 0) {
					continue;
				}
				int paletteIndex = (int) depthOut[SAMPLE_PALETTE];
				float fade = depthOut[SAMPLE_FADE];
				int colour;
				if (EchoWavePalette.isFront(depthOut[SAMPLE_DEPTH])) {
					colour = EchoWavePalette.outlineColour(paletteIndex, fade);
				} else {
					int hash = EchoTexelGrid.hash(center.getX() + localX, center.getY() + localY,
							center.getZ() + localZ, animationStep);
					if (EchoTexelGrid.isGlow(hash)) {
						colour = EchoWavePalette.glowColour(paletteIndex, fade, false);
					} else if (EchoTexelGrid.isEmber(hash)) {
						colour = EchoWavePalette.glowColour(paletteIndex, fade, true);
					} else {
						colour = body;
					}
				}
				emitTexel(consumer, matrix, basis, column * texel, row * texel, texel,
						scaleAlpha(colour, presence));
				emitted++;
			}
		}
		return emitted;
	}

	private static int scaleAlpha(int argb, float presence) {
		int alpha = Math.round(((argb >>> 24) & 0xFF) * presence);
		return (alpha << 24) | (argb & 0x00FFFFFF);
	}

	private static void emitTexel(VertexConsumer consumer, Matrix4f matrix, float[] basis,
			float u, float v, float size, int argb) {
		emitCorner(consumer, matrix, basis, u, v, argb);
		emitCorner(consumer, matrix, basis, u + size, v, argb);
		emitCorner(consumer, matrix, basis, u + size, v + size, argb);
		emitCorner(consumer, matrix, basis, u, v + size, argb);
	}

	private static void emitCorner(VertexConsumer consumer, Matrix4f matrix, float[] basis,
			float u, float v, int argb) {
		consumer.addVertex(matrix,
						basis[0] + basis[3] * u + basis[6] * v,
						basis[1] + basis[4] * u + basis[7] * v,
						basis[2] + basis[5] * u + basis[8] * v)
				.setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
	}

	public static void collectFacingWaves(List<EchoWave> nearby, int packedFace, float[] basis,
			BlockPos center, List<EchoWave> destination, int currentTick, float partialTick) {
		destination.clear();
		Direction direction = EchoFaceGeometry.direction(packedFace);
		double faceCentreX = center.getX() + basis[0] + (basis[3] + basis[6]) * 0.5D;
		double faceCentreY = center.getY() + basis[1] + (basis[4] + basis[7]) * 0.5D;
		double faceCentreZ = center.getZ() + basis[2] + (basis[5] + basis[8]) * 0.5D;
		for (EchoWave wave : nearby) {
			double facing = (wave.origin().x - faceCentreX) * direction.getStepX()
					+ (wave.origin().y - faceCentreY) * direction.getStepY()
					+ (wave.origin().z - faceCentreZ) * direction.getStepZ();
			if (facing <= 0.0D) {
				continue;
			}
			double deltaX = wave.origin().x - faceCentreX;
			double deltaY = wave.origin().y - faceCentreY;
			double deltaZ = wave.origin().z - faceCentreZ;
			double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
			float elapsed = wave.elapsedAt(currentTick, partialTick);
			double reach = wave.shellThickness() + FACE_REACH;
			if (Math.abs(distance - wave.radiusAt(elapsed)) > reach
					&& distance > wave.localRevealRadius() + FACE_REACH) {
				continue;
			}
			destination.add(wave);
		}
	}

	public static void collectNearbyWaves(EchoSectionMesh mesh, List<EchoWave> waves, BlockPos center,
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
			boolean onFront = Math.abs(distance - front) <= reach;
			boolean nearSource = distance <= EchoSectionMesh.CIRCUMRADIUS + wave.localRevealRadius();
			if (onFront || nearSource) {
				destination.add(wave);
			}
		}
	}

	public static int shadePoint(List<EchoWave> waves, int currentTick, float partialTick,
			double worldX, double worldY, double worldZ, float[] sampleOut) {
		int best = 0;
		int bestAlpha = 0;
		float bestDepth = 0.0F;
		float bestFade = 0.0F;
		int bestPalette = EchoWavePalette.PALETTE_SCULK;
		for (EchoWave wave : waves) {
			float elapsed = wave.elapsedAt(currentTick, partialTick);
			double deltaX = wave.origin().x - worldX;
			double deltaY = wave.origin().y - worldY;
			double deltaZ = wave.origin().z - worldZ;
			double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
			float fade = wave.fadeAt(elapsed);
			float depth = wave.depthOf(distance, elapsed);
			if (depth >= 0.0F) {
				int colour = EchoWavePalette.colorAt(wave.paletteIndex(), depth, fade);
				int alpha = (colour >>> 24) & 0xFF;
				if (alpha > bestAlpha) {
					bestAlpha = alpha;
					best = colour;
					bestDepth = depth;
					bestFade = fade;
					bestPalette = wave.paletteIndex();
				}
			}
			float proximity = wave.proximityAt(distance, elapsed);
			if (proximity <= 0.0F) {
				continue;
			}
			int localColour = EchoWavePalette.localColour(wave.paletteIndex(), proximity, fade);
			int localAlpha = (localColour >>> 24) & 0xFF;
			if (localAlpha > bestAlpha) {
				bestAlpha = localAlpha;
				best = localColour;
				bestDepth = EchoWavePalette.LOCAL_DEPTH;
				bestFade = fade;
				bestPalette = wave.paletteIndex();
			}
		}
		sampleOut[SAMPLE_DEPTH] = bestDepth;
		sampleOut[SAMPLE_FADE] = bestFade;
		sampleOut[SAMPLE_PALETTE] = bestPalette;
		return bestAlpha == 0 ? 0 : best;
	}

	public static int colorFromWaves(List<EchoWave> waves, int currentTick, float partialTick,
			double worldX, double worldY, double worldZ) {
		float[] sample = new float[SAMPLE_SIZE];
		int body = shadePoint(waves, currentTick, partialTick, worldX, worldY, worldZ, sample);
		if ((body >>> 24) == 0) {
			return 0;
		}
		return EchoWavePalette.glowColour((int) sample[SAMPLE_PALETTE], sample[SAMPLE_FADE], false);
	}
}
