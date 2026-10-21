package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import dev.fimkov.echoesbeneath.client.platform.EchoRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;


public final class EchoEntityRenderer {
	private static final int SILHOUETTE_LIGHT = 0;

	private EchoEntityRenderer() {
	}

	public static void render(EchoRenderContext context, ClientPlaybackSession session) {
		if (context.submits() == null || session.puppets().isEmpty()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
		float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
		Vec3 cameraPosition = context.camera().pos;
		BlockPos center = session.region().center();
		PoseStack poseStack = context.matrixStack();

		float presence = EchoDarkness.transitionProgress();

		if (presence <= 0.0F) {
			return;
		}

		for (EchoEntityPuppet puppet : session.puppets()) {
			if (!puppet.isVisibleAt(session.currentTick())) {
				continue;
			}
			puppet.updateTo(session.data().entityTimeline(), center.getX(), center.getY(), center.getZ(),
					session.currentTick(), partialTick);
			Entity entity = puppet.entity();

			renderPuppet(context, dispatcher, poseStack, entity, cameraPosition, partialTick,
					EchoReveal.of(session), presence);
		}
	}

	private static void renderPuppet(EchoRenderContext context, EntityRenderDispatcher dispatcher,
			PoseStack poseStack, Entity entity, Vec3 cameraPosition, float partialTick, float reveal,
			float presence) {
		if (entity.getBoundingBox().inflate(0.05D).contains(cameraPosition)) {
			return;
		}

		EchoSilhouetteBuffers look = EchoSilhouetteBuffers.forEntity(reveal, presence);

		if (look.isInvisible()) {
			return;
		}

		poseStack.pushPose();
		poseStack.translate(entity.getX() - cameraPosition.x,
				entity.getY() - cameraPosition.y, entity.getZ() - cameraPosition.z);
		dispatcher.submit(dispatcher.extractEntity(entity, partialTick), context.camera(),
				0.0D, 0.0D, 0.0D, poseStack, new EchoSilhouetteCollector(context.submits(), look));
		poseStack.popPose();
	}


}
