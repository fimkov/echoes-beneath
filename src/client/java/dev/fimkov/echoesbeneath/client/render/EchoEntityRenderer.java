package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import dev.fimkov.echoesbeneath.client.wave.EchoWave;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class EchoEntityRenderer {
	private static final int SILHOUETTE_LIGHT = 0;
	private static final float OUTLINE_ALPHA_SCALE = 1.0F / 255.0F;

	private EchoEntityRenderer() {
	}

	public static void render(WorldRenderContext context, ClientPlaybackSession session) {
		if (context.consumers() == null || session.puppets().isEmpty()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
		float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
		Vec3 cameraPosition = context.camera().getPosition();
		BlockPos center = session.region().center();
		List<EchoWave> waves = session.waves().active();
		PoseStack poseStack = context.matrixStack();

		boolean previousShadow = true;
		dispatcher.setRenderShadow(false);
		for (EchoEntityPuppet puppet : session.puppets()) {
			if (!puppet.isVisibleAt(session.currentTick())) {
				continue;
			}
			puppet.updateTo(session.data().entityTimeline(), center.getX(), center.getY(), center.getZ(),
					session.currentTick(), partialTick);
			Entity entity = puppet.entity();
			if (context.frustum() != null && !context.frustum().isVisible(entity.getBoundingBox().inflate(0.5D))) {
				continue;
			}
			int waveColour = waveColourFor(entity, waves, session.currentTick(), partialTick);
			renderPuppet(context, dispatcher, poseStack, entity, cameraPosition, partialTick, waveColour);
		}
		dispatcher.setRenderShadow(previousShadow);
	}

	private static void renderPuppet(WorldRenderContext context, EntityRenderDispatcher dispatcher,
			PoseStack poseStack, Entity entity, Vec3 cameraPosition, float partialTick, int waveColour) {
		EchoSilhouetteBuffers buffers = waveColour == 0
				? EchoSilhouetteBuffers.black(context.consumers())
				: EchoSilhouetteBuffers.tinted(context.consumers(), waveColour);
		poseStack.pushPose();
		poseStack.translate(entity.getX() - cameraPosition.x,
				entity.getY() - cameraPosition.y, entity.getZ() - cameraPosition.z);
		dispatcher.render(entity, 0.0D, 0.0D, 0.0D, entity.getYRot(), partialTick,
				poseStack, buffers, SILHOUETTE_LIGHT);
		poseStack.popPose();
	}

	private static int waveColourFor(Entity entity, List<EchoWave> waves, int currentTick, float partialTick) {
		if (waves.isEmpty()) {
			return 0;
		}
		Vec3 probe = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
		return EchoWaveRenderer.colorFromWaves(waves, currentTick, partialTick, probe.x, probe.y, probe.z);
	}

}
