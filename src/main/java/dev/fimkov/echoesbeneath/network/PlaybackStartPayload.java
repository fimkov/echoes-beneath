package dev.fimkov.echoesbeneath.network;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public record PlaybackStartPayload(
		UUID playbackId,
		ResourceKey<Level> dimension,
		BlockPos center,
		int radius,
		int verticalRadius,
		int durationTicks,
		int memorySlot,
		int vision) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PlaybackStartPayload> TYPE =
			new CustomPacketPayload.Type<>(EchoesBeneath.id("playback_start"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PlaybackStartPayload> STREAM_CODEC =
			StreamCodec.of(PlaybackStartPayload::encode, PlaybackStartPayload::decode);

	private static void encode(RegistryFriendlyByteBuf buffer, PlaybackStartPayload payload) {
		buffer.writeUUID(payload.playbackId);
		buffer.writeIdentifier(payload.dimension.identifier());
		buffer.writeBlockPos(payload.center);
		buffer.writeVarInt(payload.radius);
		buffer.writeVarInt(payload.verticalRadius);
		buffer.writeVarInt(payload.durationTicks);
		buffer.writeVarInt(payload.memorySlot);
		buffer.writeVarInt(payload.vision);
	}

	private static PlaybackStartPayload decode(RegistryFriendlyByteBuf buffer) {
		UUID playbackId = buffer.readUUID();
		Identifier dimensionId = buffer.readIdentifier();
		BlockPos center = buffer.readBlockPos();
		int radius = clamp(buffer.readVarInt(), EchoPayloadLimits.MAX_RADIUS);
		int verticalRadius = clamp(buffer.readVarInt(), EchoPayloadLimits.MAX_RADIUS);
		int durationTicks = clamp(buffer.readVarInt(), EchoPayloadLimits.MAX_DURATION_TICKS);
		int memorySlot = clamp(buffer.readVarInt(), Integer.MAX_VALUE);
		int vision = clamp(buffer.readVarInt(), EchoLensUpgrades.MAX_VISION);

		return new PlaybackStartPayload(playbackId, ResourceKey.create(Registries.DIMENSION, dimensionId),
				center, radius, verticalRadius, durationTicks, memorySlot, vision);
	}

	private static int clamp(int value, int maximum) {
		if (value < 0 || value > maximum) {
			throw new IllegalArgumentException("Playback field " + value + " is outside the accepted range");
		}

		return value;
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
