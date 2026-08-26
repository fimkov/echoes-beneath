package dev.fimkov.echoesbeneath.network;

import dev.fimkov.echoesbeneath.platform.EchoPlatforms;

public final class EchoNetworkPayloads {
	private EchoNetworkPayloads() {
	}

	public static void registerCommon() {
		EchoPlatforms.network().serverToClient(PlaybackStartPayload.TYPE, PlaybackStartPayload.STREAM_CODEC);
		EchoPlatforms.network().serverToClient(PlaybackDataPayload.TYPE, PlaybackDataPayload.STREAM_CODEC);
		EchoPlatforms.network().serverToClient(PlaybackTickPayload.TYPE, PlaybackTickPayload.STREAM_CODEC);
		EchoPlatforms.network().serverToClient(PlaybackStopPayload.TYPE, PlaybackStopPayload.STREAM_CODEC);
		EchoPlatforms.network().serverToClient(ChartShapePayload.TYPE, ChartShapePayload.STREAM_CODEC);
	}
}
