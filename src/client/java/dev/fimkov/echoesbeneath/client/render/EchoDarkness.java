package dev.fimkov.echoesbeneath.client.render;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.client.config.EchoClientConfig;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackManager;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import java.util.Optional;
import net.minecraft.client.Minecraft;

public final class EchoDarkness {
	public static final int EMISSIVE_TEXEL = 15;
	public static final float FULLY_DARK = 1.0F;

	private static final float VISION_RELIEF = 0.55F;
	private static final float ACTIVE_THRESHOLD = 0.002F;
	private static final float SKY_HIDDEN_THRESHOLD = 0.9F;
	private static final float FOG_START_FRACTION = 0.05F;
	private static final float FOG_END_MARGIN = 4.0F;

	private static ClientPlaybackSession enclosing;
	private static float darkness;
	private static float lastTarget = FULLY_DARK;
	private static float lastFogStart;
	private static float lastFogEnd;
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
		if (found != null) {
			lastFogStart = found.region().radius() * FOG_START_FRACTION;
			lastFogEnd = Math.max(FOG_END_MARGIN, found.region().radius() - FOG_END_MARGIN);
		}
	}

	public static void advanceTransition(float realtimeDeltaTicks) {
		float target = enclosing == null ? 0.0F : darknessTargetFor(enclosing);
		float step = Math.max(0.0F, realtimeDeltaTicks) * EchoClientConfig.values().transitionPerTick();
		if (darkness < target) {
			darkness = Math.min(target, darkness + step);
		} else {
			darkness = Math.max(target, darkness - step);
		}
	}

	private static float darknessTargetFor(ClientPlaybackSession session) {
		float relief = (float) session.vision()
				/ EchoLensUpgrades.MAX_VISION * VISION_RELIEF;
		lastTarget = (FULLY_DARK - relief)
				* EchoClientConfig.values().darknessFactor();
		return lastTarget;
	}

	public static float transitionProgress() {
		if (lastTarget <= ACTIVE_THRESHOLD) {
			return 0.0F;
		}
		return Math.clamp(darkness / lastTarget, 0.0F, 1.0F);
	}

	public static float darkness() {
		return darkness;
	}

	public static boolean isTransitionActive() {
		return darkness > ACTIVE_THRESHOLD;
	}

	public static boolean shouldHideSky() {
		if (!EchoClientConfig.values().hideSkyDuringPlayback()) {
			return false;
		}
		return darkness >= SKY_HIDDEN_THRESHOLD;
	}

	public static float regionFogStart() {
		return lastFogStart;
	}

	public static float regionFogEnd() {
		return lastFogEnd;
	}

	public static Optional<ClientPlaybackSession> enclosingSession() {
		return Optional.ofNullable(enclosing);
	}

	public static boolean isViewerInsideRegion() {
		return enclosing != null;
	}

	public static void clearFrameState() {
		enclosing = null;
		darkness = 0.0F;
	}

	public static void applyDarkness(float partialTick) {
		if (restoring) {
			return;
		}
		lightTextureAccess().ifPresent(access -> access.echoesBeneath$applyDarkness(darkness, partialTick));
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

	private static Optional<EchoLightTextureAccess> lightTextureAccess() {
		Minecraft client = Minecraft.getInstance();
		if (client.gameRenderer == null
				|| !(client.gameRenderer.lightTexture() instanceof EchoLightTextureAccess access)) {
			return Optional.empty();
		}
		return Optional.of(access);
	}
}
