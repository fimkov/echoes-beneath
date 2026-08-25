package dev.fimkov.echoesbeneath.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class EchoNetworkPayloads {
	private EchoNetworkPayloads() {
	}

	public static void registerCommon() {
		PayloadTypeRegistry.playS2C().register(PlaybackStartPayload.TYPE, PlaybackStartPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(PlaybackDataPayload.TYPE, PlaybackDataPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(PlaybackTickPayload.TYPE, PlaybackTickPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(PlaybackStopPayload.TYPE, PlaybackStopPayload.STREAM_CODEC);
	}
}
