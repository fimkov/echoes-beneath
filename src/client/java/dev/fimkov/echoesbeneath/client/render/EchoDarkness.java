package dev.fimkov.echoesbeneath.client.render;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackManager;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import net.minecraft.client.Minecraft;

import java.util.Optional;

public final class EchoDarkness {
	public static final int VOID_LIGHT_PIXEL = 0xFF000000;
	public static final int EMISSIVE_TEXEL = 15;
	private static final float FOG_START_FRACTION = 0.05F;
	private static final float FOG_END_MARGIN = 4.0F;

	private static ClientPlaybackSession enclosing;
	private static boolean restoring;

	private EchoDarkness() {
	}

	public static void refreshFrameState() {
		Minecraft client = Minecraft.getInstance();
		ClientPlaybackSession found = null;
		if (client.level != null && client.player != null) {
			found = ClientPlaybackManager.instance()
					.sessionContaining(client.level, client.player.getEyePosition()).orElse(null);
		}
		if (found != enclosing) {
			EchoesBeneath.LOGGER.info("Echo region {}", found == null ? "left" : "entered");
		}
		enclosing = found;
	}

	public static void clearFrameState() {
		enclosing = null;
	}

	public static Optional<ClientPlaybackSession> enclosingSession() {
		return Optional.ofNullable(enclosing);
	}

	public static boolean isViewerInsideRegion() {
		return enclosing != null;
	}

	public static boolean shouldDarken() {
		return !restoring && isViewerInsideRegion();
	}

	public static void paintVoidLight() {
		lightTextureAccess().ifPresent(EchoLightTextureAccess::echoesBeneath$paintVoid);
	}

	private static java.util.Optional<EchoLightTextureAccess> lightTextureAccess() {
		Minecraft client = Minecraft.getInstance();
		if (client.gameRenderer == null
				|| !(client.gameRenderer.lightTexture() instanceof EchoLightTextureAccess access)) {
			return Optional.empty();
		}
		return Optional.of(access);
	}

	public static void restoreNaturalLight(float partialTick) {
		Optional<EchoLightTextureAccess> access = lightTextureAccess();
		if (access.isEmpty()) {
			return;
		}
		restoring = true;
		try {
			access.get().echoesBeneath$restoreNaturalLight(partialTick);
		} finally {
			restoring = false;
		}
	}

	public static float fogStartFor(ClientPlaybackSession session) {
		return session.region().radius() * FOG_START_FRACTION;
	}

	public static float fogEndFor(ClientPlaybackSession session) {
		return Math.max(FOG_END_MARGIN, session.region().radius() - FOG_END_MARGIN);
	}
}
