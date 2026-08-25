package dev.fimkov.echoesbeneath.client.render;

import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackManager;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class EchoDarkness {
	public static final int SILHOUETTE_LIGHT_PIXEL = 0xFF000000;
	public static final int FULL_BRIGHT_TEXEL = 15;
	private static final float FOG_START_FRACTION = 0.05F;
	private static final float FOG_END_MARGIN = 4.0F;

	private EchoDarkness() {
	}

	public static Optional<ClientPlaybackSession> enclosingSession() {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) {
			return Optional.empty();
		}
		Vec3 viewpoint = client.gameRenderer.getMainCamera().getPosition();
		return ClientPlaybackManager.instance().sessionContaining(client.level, viewpoint);
	}

	public static boolean isViewerInsideRegion() {
		return enclosingSession().isPresent();
	}

	public static float fogStartFor(ClientPlaybackSession session) {
		return session.region().radius() * FOG_START_FRACTION;
	}

	public static float fogEndFor(ClientPlaybackSession session) {
		return Math.max(FOG_END_MARGIN, session.region().radius() - FOG_END_MARGIN);
	}
}
