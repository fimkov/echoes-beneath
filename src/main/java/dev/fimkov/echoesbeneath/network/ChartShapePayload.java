package dev.fimkov.echoesbeneath.network;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ChartShapePayload(int entityId, int chunkIndex, int chunkCount, byte[] chunk)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<ChartShapePayload> TYPE =
			new CustomPacketPayload.Type<>(EchoesBeneath.id("chart_shape"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ChartShapePayload> STREAM_CODEC =
			StreamCodec.of(ChartShapePayload::encode, ChartShapePayload::decode);

	private static void encode(RegistryFriendlyByteBuf buffer, ChartShapePayload payload) {
		buffer.writeVarInt(payload.entityId);
		buffer.writeVarInt(payload.chunkIndex);
		buffer.writeVarInt(payload.chunkCount);
		buffer.writeByteArray(payload.chunk);
	}

	private static ChartShapePayload decode(RegistryFriendlyByteBuf buffer) {
		int entityId = buffer.readVarInt();
		int chunkIndex = buffer.readVarInt();
		int chunkCount = buffer.readVarInt();
		if (chunkCount <= 0 || chunkCount > EchoPayloadLimits.MAX_CHUNKS_PER_PART
				|| chunkIndex < 0 || chunkIndex >= chunkCount) {
			throw new IllegalArgumentException("Chart chunk " + chunkIndex + "/" + chunkCount + " is invalid");
		}
		byte[] chunk = buffer.readByteArray(EchoPayloadLimits.MAX_CHUNK_BYTES);
		return new ChartShapePayload(entityId, chunkIndex, chunkCount, chunk);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
