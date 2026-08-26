package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import dev.fimkov.echoesbeneath.client.platform.EchoRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;


public final class EchoEntityRenderer {
	private static final int SILHOUETTE_LIGHT = 0;

	private EchoEntityRenderer() {
	}

	public static void render(EchoRenderContext context, ClientPlaybackSession session) {
		if (context.consumers() == null || session.puppets().isEmpty()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
		float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
		Vec3 cameraPosition = context.camera().getPosition();
		BlockPos center = session.region().center();
		PoseStack poseStack = context.matrixStack();

		float presence = EchoDarkness.transitionProgress();

		if (presence <= 0.0F) {
			return;
		}

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
			renderPuppet(context, dispatcher, poseStack, entity, cameraPosition, partialTick,
					EchoReveal.of(session), presence);
		}

		dispatcher.setRenderShadow(previousShadow);
	}

	private static void renderPuppet(EchoRenderContext context, EntityRenderDispatcher dispatcher,
			PoseStack poseStack, Entity entity, Vec3 cameraPosition, float partialTick, float reveal,
			float presence) {
		EchoSilhouetteBuffers buffers = EchoSilhouetteBuffers.forEntity(context.consumers(),
				textureOf(dispatcher, entity), reveal, presence);
		poseStack.pushPose();
		poseStack.translate(entity.getX() - cameraPosition.x,
				entity.getY() - cameraPosition.y, entity.getZ() - cameraPosition.z);
		dispatcher.render(entity, 0.0D, 0.0D, 0.0D, entity.getYRot(), partialTick,
				poseStack, buffers, SILHOUETTE_LIGHT);
		poseStack.popPose();
	}


	private static ResourceLocation textureOf(EntityRenderDispatcher dispatcher, Entity entity) {
		return castRenderer(dispatcher.getRenderer(entity)).getTextureLocation(entity);
	}

	@SuppressWarnings("unchecked")
	private static EntityRenderer<Entity> castRenderer(EntityRenderer<?> renderer) {
		return (EntityRenderer<Entity>) renderer;
	}
}
