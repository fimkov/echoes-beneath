package dev.fimkov.echoesbeneath.network;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RecordSoundPayload(UUID playback, BlockPos speaker, int voice, ResourceLocation sound,
		float volume, float pitch, int action, int offsetTicks) implements CustomPacketPayload {
	public static final int PLAY = 0;
	public static final int STOP_VOICE = 1;
	public static final int STOP_ALL = 2;
	public static final Type<RecordSoundPayload> TYPE = new Type<>(EchoesBeneath.id("record_sound"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RecordSoundPayload> STREAM_CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeUUID(payload.playback);
				buffer.writeBlockPos(payload.speaker);
				buffer.writeVarInt(payload.voice);
				buffer.writeResourceLocation(payload.sound);
				buffer.writeFloat(payload.volume);
				buffer.writeFloat(payload.pitch);
				buffer.writeVarInt(payload.action);
				buffer.writeVarInt(payload.offsetTicks);
			}, buffer -> new RecordSoundPayload(buffer.readUUID(), buffer.readBlockPos(), buffer.readVarInt(),
					buffer.readResourceLocation(), Math.clamp(buffer.readFloat(), 0.0F, 16.0F),
					Math.clamp(buffer.readFloat(), 0.1F, 4.0F), buffer.readVarInt(), Math.clamp(buffer.readVarInt(), 0, 2400)));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
