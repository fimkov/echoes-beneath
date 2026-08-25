package dev.fimkov.echoesbeneath.client.render;

import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;

public final class EchoReveal {
	private EchoReveal() {
	}

	public static float of(ClientPlaybackSession session) {
		return (float) session.clarity() / EchoLensUpgrades.MAX_CLARITY;
	}
}
