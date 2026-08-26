package dev.fimkov.echoesbeneath.chart;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

public record EchoChartData(UUID memoryId, ResourceKey<Level> dimension, BlockPos center,
		int radius, int lowestY, int highestY, int soundedBlocks) {

	public static final Codec<EchoChartData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("memory").forGetter(EchoChartData::memoryId),
			ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(EchoChartData::dimension),
			BlockPos.CODEC.fieldOf("center").forGetter(EchoChartData::center),
			Codec.INT.fieldOf("radius").forGetter(EchoChartData::radius),
			Codec.INT.fieldOf("lowest_y").forGetter(EchoChartData::lowestY),
			Codec.INT.fieldOf("highest_y").forGetter(EchoChartData::highestY),
			Codec.INT.fieldOf("sounded").forGetter(EchoChartData::soundedBlocks)
	).apply(instance, EchoChartData::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, EchoChartData> STREAM_CODEC = StreamCodec.of(
			(buffer, value) -> {
				buffer.writeUUID(value.memoryId);
				buffer.writeResourceKey(value.dimension);
				buffer.writeBlockPos(value.center);
				buffer.writeVarInt(value.radius);
				buffer.writeInt(value.lowestY);
				buffer.writeInt(value.highestY);
				buffer.writeVarInt(value.soundedBlocks);
			},
			buffer -> new EchoChartData(
					buffer.readUUID(),
					buffer.readResourceKey(Registries.DIMENSION),
					buffer.readBlockPos(),
					buffer.readVarInt(),
					buffer.readInt(),
					buffer.readInt(),
					buffer.readVarInt()));

	public int heightSpan() {
		return highestY - lowestY + 1;
	}
}
