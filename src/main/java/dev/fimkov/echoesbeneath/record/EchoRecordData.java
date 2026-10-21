package dev.fimkov.echoesbeneath.record;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record EchoRecordData(UUID memoryId, int startTick, int endTick, int volume) {
	public static final int MAX_TICKS = 2400;
	public static final int MAX_VOLUME = 100;
	public static final Codec<EchoRecordData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("memory").forGetter(EchoRecordData::memoryId),
			Codec.intRange(0, MAX_TICKS - 1).fieldOf("start").forGetter(EchoRecordData::startTick),
			Codec.intRange(1, MAX_TICKS).fieldOf("end").forGetter(EchoRecordData::endTick),
			Codec.intRange(0, MAX_VOLUME).fieldOf("volume").forGetter(EchoRecordData::volume)
	).apply(instance, EchoRecordData::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, EchoRecordData> STREAM_CODEC = StreamCodec.of(
			(buffer, data) -> {
				buffer.writeUUID(data.memoryId);
				buffer.writeVarInt(data.startTick);
				buffer.writeVarInt(data.endTick);
				buffer.writeVarInt(data.volume);
			}, buffer -> new EchoRecordData(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));

	public EchoRecordData {
		if (memoryId == null || startTick < 0 || startTick >= endTick || endTick > MAX_TICKS
				|| volume < 0 || volume > MAX_VOLUME) {
			throw new IllegalArgumentException("Invalid echo record edit");
		}
	}

	public static EchoRecordData edited(UUID memoryId, int duration, int start, int end, int volume) {
		int limit = Math.clamp(duration, 1, MAX_TICKS);
		int first = Math.clamp(start, 0, limit - 1);
		return new EchoRecordData(memoryId, first, Math.clamp(end, first + 1, limit),
				Math.clamp(volume, 0, MAX_VOLUME));
	}

	public int durationTicks() {
		return endTick - startTick;
	}

	public float gain() {
		return volume / 100.0F;
	}
}
