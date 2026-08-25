package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackManager;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public final class EchoPlaybackRenderer {
	private static final double SECTION_RENDER_DISTANCE = 96.0D;

	private EchoPlaybackRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_ENTITIES.register(EchoPlaybackRenderer::render);
	}

	private static void render(WorldRenderContext context) {
		ClientPlaybackManager manager = ClientPlaybackManager.instance();
		if (manager.sessions().isEmpty() || context.world() == null || context.matrixStack() == null) {
			return;
		}
		Vec3 cameraPosition = context.camera().getPosition();
		for (ClientPlaybackSession session : manager.sessions()) {
			if (!session.region().dimension().equals(context.world().dimension())) {
				continue;
			}
			boolean cameraInside = session.region().contains(context.world().dimension(), cameraPosition);
			EchoBoundaryRenderer.render(context, session, cameraInside);
			if (!cameraInside) {
				continue;
			}
			session.buildPendingMeshes();
			renderSnapshotMeshes(context, session, cameraPosition);
			EchoBlockDeltaRenderer.render(context, session, cameraPosition);
			EchoWaveRenderer.render(context, session, cameraPosition);
			EchoEntityRenderer.render(context, session);
		}
	}

	private static void renderSnapshotMeshes(WorldRenderContext context, ClientPlaybackSession session,
			Vec3 cameraPosition) {
		ShaderInstance shader = GameRenderer.getPositionColorShader();
		if (shader == null) {
			return;
		}
		BlockPos center = session.region().center();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		PoseStack poseStack = context.matrixStack();
		for (Map.Entry<Integer, EchoSectionMesh> entry : session.meshes().entrySet()) {
			EchoSectionMesh mesh = entry.getValue();
			if (!isSectionVisible(context, mesh, center, cameraPosition)) {
				continue;
			}
			VertexBuffer buffer = mesh.uploadedBuffer();
			if (buffer == null) {
				continue;
			}
			poseStack.pushPose();
			poseStack.translate(
					center.getX() + mesh.originX() - cameraPosition.x,
					center.getY() + mesh.originY() - cameraPosition.y,
					center.getZ() + mesh.originZ() - cameraPosition.z);
			buffer.bind();
			buffer.drawWithShader(poseStack.last().pose(), context.projectionMatrix(), shader);
			VertexBuffer.unbind();
			poseStack.popPose();
		}
		RenderSystem.disableBlend();
	}

	private static boolean isSectionVisible(WorldRenderContext context, EchoSectionMesh mesh,
			BlockPos center, Vec3 cameraPosition) {
		double sectionCenterX = center.getX() + mesh.originX() + 8.0D;
		double sectionCenterY = center.getY() + mesh.originY() + 8.0D;
		double sectionCenterZ = center.getZ() + mesh.originZ() + 8.0D;
		double distanceSquared = cameraPosition.distanceToSqr(sectionCenterX, sectionCenterY, sectionCenterZ);
		if (distanceSquared > SECTION_RENDER_DISTANCE * SECTION_RENDER_DISTANCE) {
			return false;
		}
		if (context.frustum() == null) {
			return true;
		}
		return context.frustum().isVisible(mesh.boundsAround(center.getX(), center.getY(), center.getZ()));
	}

	public static boolean isCameraInsideAnyRegion() {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null) {
			return false;
		}
		return ClientPlaybackManager.instance()
				.sessionContaining(client.level, client.player.getEyePosition()).isPresent();
	}
}
