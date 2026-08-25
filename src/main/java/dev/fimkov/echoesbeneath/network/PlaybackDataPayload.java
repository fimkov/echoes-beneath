package dev.fimkov.echoesbeneath.network;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record PlaybackDataPayload(
		UUID playbackId,
		EchoPlaybackDataPart part,
		int chunkIndex,
		int chunkCount,
		byte[] chunk) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PlaybackDataPayload> TYPE =
			new CustomPacketPayload.Type<>(EchoesBeneath.id("playback_data"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PlaybackDataPayload> STREAM_CODEC =
			StreamCodec.of(PlaybackDataPayload::encode, PlaybackDataPayload::decode);

	private static void encode(RegistryFriendlyByteBuf buffer, PlaybackDataPayload payload) {
		buffer.writeUUID(payload.playbackId);
		buffer.writeVarInt(payload.part.ordinal());
		buffer.writeVarInt(payload.chunkIndex);
		buffer.writeVarInt(payload.chunkCount);
		buffer.writeByteArray(payload.chunk);
	}

	private static PlaybackDataPayload decode(RegistryFriendlyByteBuf buffer) {
		UUID playbackId = buffer.readUUID();
		EchoPlaybackDataPart part = EchoPlaybackDataPart.fromIndex(buffer.readVarInt());
		int chunkIndex = buffer.readVarInt();
		int chunkCount = buffer.readVarInt();
		if (chunkCount <= 0 || chunkCount > EchoPayloadLimits.MAX_CHUNKS_PER_PART
				|| chunkIndex < 0 || chunkIndex >= chunkCount) {
			throw new IllegalArgumentException("Playback data chunk " + chunkIndex + "/" + chunkCount + " is invalid");
		}
		byte[] chunk = buffer.readByteArray(EchoPayloadLimits.MAX_CHUNK_BYTES);
		return new PlaybackDataPayload(playbackId, part, chunkIndex, chunkCount, chunk);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
