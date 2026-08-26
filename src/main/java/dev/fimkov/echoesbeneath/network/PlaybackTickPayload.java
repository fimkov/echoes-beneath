package dev.fimkov.echoesbeneath.network;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record PlaybackTickPayload(UUID playbackId, int currentTick) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PlaybackTickPayload> TYPE =
			new CustomPacketPayload.Type<>(EchoesBeneath.id("playback_tick"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PlaybackTickPayload> STREAM_CODEC =
			StreamCodec.of(
					(buffer, payload) -> {
						buffer.writeUUID(payload.playbackId);
						buffer.writeVarInt(payload.currentTick);
					},
					buffer -> {
						UUID playbackId = buffer.readUUID();
						int tick = buffer.readVarInt();

						if (tick < 0 || tick > EchoPayloadLimits.MAX_DURATION_TICKS) {
							throw new IllegalArgumentException("Playback tick " + tick + " is outside the accepted range");
						}

						return new PlaybackTickPayload(playbackId, tick);
					});

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
