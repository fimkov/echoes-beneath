package dev.fimkov.echoesbeneath.client.playback;

import dev.fimkov.echoesbeneath.network.PlaybackDataPayload;
import dev.fimkov.echoesbeneath.network.PlaybackStartPayload;
import dev.fimkov.echoesbeneath.network.PlaybackStopPayload;
import dev.fimkov.echoesbeneath.network.PlaybackTickPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class EchoPlaybackReceiver {
	private EchoPlaybackReceiver() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(PlaybackStartPayload.TYPE,
				(payload, context) -> context.client().execute(
						() -> ClientPlaybackManager.instance().onStart(payload)));

		ClientPlayNetworking.registerGlobalReceiver(PlaybackDataPayload.TYPE,
				(payload, context) -> context.client().execute(() -> {
					if (context.client().level != null) {
						ClientPlaybackManager.instance().onData(payload, context.client().level);
					}
				}));

		ClientPlayNetworking.registerGlobalReceiver(PlaybackTickPayload.TYPE,
				(payload, context) -> context.client().execute(
						() -> ClientPlaybackManager.instance().onTick(payload)));

		ClientPlayNetworking.registerGlobalReceiver(PlaybackStopPayload.TYPE,
				(payload, context) -> context.client().execute(
						() -> ClientPlaybackManager.instance().onStop(payload)));

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientPlaybackManager.instance().clearAll();
			dev.fimkov.echoesbeneath.client.render.EchoDarkness.clearFrameState();
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level != null && !client.isPaused()) {
				ClientPlaybackManager.instance().clientTick(client.level);
			}
			dev.fimkov.echoesbeneath.client.render.EchoDarkness.refreshFrameState();
		});
	}
}
