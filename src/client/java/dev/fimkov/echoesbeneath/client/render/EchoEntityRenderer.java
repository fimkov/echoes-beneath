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
			renderPuppet(context, dispatcher, poseStack, entity, cameraPosition, partialTick);
			renderWaveOutline(context, poseStack, entity, waves, session.currentTick(), partialTick, cameraPosition);
		}
		dispatcher.setRenderShadow(previousShadow);
	}

	private static void renderPuppet(WorldRenderContext context, EntityRenderDispatcher dispatcher,
			PoseStack poseStack, Entity entity, Vec3 cameraPosition, float partialTick) {
		poseStack.pushPose();
		poseStack.translate(entity.getX() - cameraPosition.x,
				entity.getY() - cameraPosition.y, entity.getZ() - cameraPosition.z);
		dispatcher.render(entity, 0.0D, 0.0D, 0.0D, entity.getYRot(), partialTick,
				poseStack, new EchoSilhouetteBuffers(context.consumers()), SILHOUETTE_LIGHT);
		poseStack.popPose();
	}

	private static void renderWaveOutline(WorldRenderContext context, PoseStack poseStack, Entity entity,
			List<EchoWave> waves, int currentTick, float partialTick, Vec3 cameraPosition) {
		if (waves.isEmpty()) {
			return;
		}
		Vec3 probe = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
		int color = EchoWaveRenderer.colorFromWaves(waves, currentTick, partialTick, probe.x, probe.y, probe.z);
		if (color == 0) {
			return;
		}
		AABB box = entity.getBoundingBox().move(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		LevelRenderer.renderLineBox(poseStack, context.consumers().getBuffer(RenderType.lines()), box,
				((color >> 16) & 0xFF) * OUTLINE_ALPHA_SCALE,
				((color >> 8) & 0xFF) * OUTLINE_ALPHA_SCALE,
				(color & 0xFF) * OUTLINE_ALPHA_SCALE,
				((color >>> 24) & 0xFF) * OUTLINE_ALPHA_SCALE);
	}
}
