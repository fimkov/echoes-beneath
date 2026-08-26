package dev.fimkov.echoesbeneath.network;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ChartShapeRequestPayload(BlockPos position) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ChartShapeRequestPayload> TYPE =
			new CustomPacketPayload.Type<>(EchoesBeneath.id("chart_shape_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ChartShapeRequestPayload> STREAM_CODEC =
			StreamCodec.of(
					(buffer, payload) -> buffer.writeBlockPos(payload.position),
					buffer -> new ChartShapeRequestPayload(buffer.readBlockPos()));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
