package dev.fimkov.echoesbeneath.client.render;

import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackManager;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.world.phys.Vec3;

public final class EchoPlaybackRenderer {
	private EchoPlaybackRenderer() {
	}

	public static void register() {
		WorldRenderEvents.START.register(context -> {
			EchoDarkness.refreshFrameState();
			if (EchoDarkness.isViewerInsideRegion()) {
				EchoDarkness.paintVoidLight();
			}
		});
		WorldRenderEvents.AFTER_ENTITIES.register(EchoPlaybackRenderer::render);
		WorldRenderEvents.END.register(EchoPlaybackRenderer::restoreInterfaceLight);
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
			boolean cameraInside = EchoDarkness.enclosingSession()
					.filter(enclosing -> enclosing == session)
					.isPresent();
			EchoBoundaryRenderer.render(context, session, cameraInside);
			if (!cameraInside) {
				continue;
			}
			session.buildPendingMeshes();
			EchoBlockDeltaRenderer.render(context, session, cameraPosition);
			EchoWaveRenderer.render(context, session, cameraPosition);
			EchoEntityRenderer.render(context, session);
		}
	}

	private static void restoreInterfaceLight(WorldRenderContext context) {
		if (!EchoDarkness.isViewerInsideRegion()) {
			return;
		}
		EchoDarkness.restoreNaturalLight(context.tickCounter().getGameTimeDeltaPartialTick(false));
	}
}
