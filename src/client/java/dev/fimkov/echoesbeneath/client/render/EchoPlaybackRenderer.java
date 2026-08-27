package dev.fimkov.echoesbeneath.client.render;

import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackManager;
import dev.fimkov.echoesbeneath.client.sound.EchoRegionAmbience;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import dev.fimkov.echoesbeneath.client.platform.EchoRenderContext;
import net.minecraft.world.phys.Vec3;

public final class EchoPlaybackRenderer {
	private EchoPlaybackRenderer() {
	}

	public static void frameStart(EchoRenderContext context) {
		EchoDarkness.refreshFrameState();
		EchoDarkness.advanceTransition(context.tickCounter().getRealtimeDeltaTicks());

		EchoRegionAmbience.follow();
	}

	public static void afterEntities(EchoRenderContext context) {
		ClientPlaybackManager manager = ClientPlaybackManager.instance();

		if (manager.sessions().isEmpty() || context.world() == null || context.matrixStack() == null) {
			return;
		}

		Vec3 cameraPosition = context.camera().pos;

		for (ClientPlaybackSession session : manager.sessions()) {
			if (!session.region().dimension().equals(context.world().dimension())) {
				continue;
			}
			boolean cameraInside = EchoDarkness.enclosingSession()
					.filter(enclosing -> enclosing == session)
					.isPresent();

			if (!cameraInside) {
				continue;
			}
			session.buildPendingMeshes();
			EchoBlockHologramRenderer.render(context, session, cameraPosition);
			EchoBlockDeltaRenderer.render(context, session, cameraPosition);
			EchoWaveRenderer.render(context, session, cameraPosition);
			EchoEntityRenderer.render(context, session);
		}
	}

	public static void frameEnd(EchoRenderContext context) {
	}
}
