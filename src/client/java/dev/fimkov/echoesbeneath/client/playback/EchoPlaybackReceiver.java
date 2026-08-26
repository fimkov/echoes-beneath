package dev.fimkov.echoesbeneath.client.playback;

import dev.fimkov.echoesbeneath.client.platform.EchoClientPlatforms;
import dev.fimkov.echoesbeneath.client.render.EchoDarkness;
import dev.fimkov.echoesbeneath.network.PlaybackDataPayload;
import dev.fimkov.echoesbeneath.client.chart.ClientChartShapes;
import dev.fimkov.echoesbeneath.network.ChartShapePayload;
import dev.fimkov.echoesbeneath.network.PlaybackStartPayload;
import dev.fimkov.echoesbeneath.network.PlaybackStopPayload;
import dev.fimkov.echoesbeneath.network.PlaybackTickPayload;
import net.minecraft.client.Minecraft;

public final class EchoPlaybackReceiver {
	private EchoPlaybackReceiver() {
	}

	public static void register() {
		EchoClientPlatforms.get().receive(PlaybackStartPayload.TYPE,
				payload -> ClientPlaybackManager.instance().onStart(payload));

		EchoClientPlatforms.get().receive(PlaybackDataPayload.TYPE, payload -> {
			Minecraft client = Minecraft.getInstance();
			if (client.level != null) {
				ClientPlaybackManager.instance().onData(payload, client.level);
			}
		});

		EchoClientPlatforms.get().receive(PlaybackTickPayload.TYPE,
				payload -> ClientPlaybackManager.instance().onTick(payload));

		EchoClientPlatforms.get().receive(PlaybackStopPayload.TYPE,
				payload -> ClientPlaybackManager.instance().onStop(payload));

		EchoClientPlatforms.get().receive(ChartShapePayload.TYPE, ClientChartShapes::accept);

		EchoClientPlatforms.get().onDisconnect(() -> {
			ClientChartShapes.forget();
			ClientPlaybackManager.instance().clearAll();
			EchoDarkness.clearFrameState();
		});

		EchoClientPlatforms.get().onClientTick(() -> {
			Minecraft client = Minecraft.getInstance();
			if (client.level != null && !client.isPaused()) {
				ClientPlaybackManager.instance().clientTick(client.level);
			}
			EchoDarkness.refreshFrameState();
			dev.fimkov.echoesbeneath.client.input.EchoKeys.poll();
		});
	}
}
