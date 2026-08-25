package dev.fimkov.echoesbeneath.network;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record PlaybackStopPayload(UUID playbackId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PlaybackStopPayload> TYPE =
			new CustomPacketPayload.Type<>(EchoesBeneath.id("playback_stop"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PlaybackStopPayload> STREAM_CODEC =
			StreamCodec.of(
					(buffer, payload) -> buffer.writeUUID(payload.playbackId),
					buffer -> new PlaybackStopPayload(buffer.readUUID()));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
