package dev.fimkov.echoesbeneath.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.UUID;

public record EchoMemoryReference(
		UUID memoryId,
		int slot,
		ResourceKey<Level> dimension,
		BlockPos center,
		int radius,
		int verticalRadius,
		int durationTicks,
		long createdAtEpochMillis) {

	private static final Codec<ResourceKey<Level>> DIMENSION_CODEC = ResourceKey.codec(Registries.DIMENSION);

	public static final Codec<EchoMemoryReference> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("memory_id").forGetter(EchoMemoryReference::memoryId),
			Codec.intRange(0, EchoFormat.MEMORY_SLOT_COUNT - 1).fieldOf("slot").forGetter(EchoMemoryReference::slot),
			DIMENSION_CODEC.fieldOf("dimension").forGetter(EchoMemoryReference::dimension),
			BlockPos.CODEC.fieldOf("center").forGetter(EchoMemoryReference::center),
			Codec.INT.fieldOf("radius").forGetter(EchoMemoryReference::radius),
			Codec.INT.fieldOf("vertical_radius").forGetter(EchoMemoryReference::verticalRadius),
			Codec.INT.fieldOf("duration_ticks").forGetter(EchoMemoryReference::durationTicks),
			Codec.LONG.fieldOf("created_at").forGetter(EchoMemoryReference::createdAtEpochMillis)
	).apply(instance, EchoMemoryReference::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, EchoMemoryReference> STREAM_CODEC = StreamCodec.of(
			EchoMemoryReference::encode, EchoMemoryReference::decode);

	private static void encode(RegistryFriendlyByteBuf buffer, EchoMemoryReference reference) {
		buffer.writeUUID(reference.memoryId);
		buffer.writeVarInt(reference.slot);
		buffer.writeResourceLocation(reference.dimension.location());
		buffer.writeBlockPos(reference.center);
		buffer.writeVarInt(reference.radius);
		buffer.writeVarInt(reference.verticalRadius);
		buffer.writeVarInt(reference.durationTicks);
		buffer.writeLong(reference.createdAtEpochMillis);
	}

	private static EchoMemoryReference decode(RegistryFriendlyByteBuf buffer) {
		UUID memoryId = buffer.readUUID();
		int slot = buffer.readVarInt();
		ResourceLocation dimensionId = buffer.readResourceLocation();
		BlockPos center = buffer.readBlockPos();
		int radius = buffer.readVarInt();
		int verticalRadius = buffer.readVarInt();
		int durationTicks = buffer.readVarInt();
		long createdAt = buffer.readLong();
		return new EchoMemoryReference(memoryId, slot, ResourceKey.create(Registries.DIMENSION, dimensionId),
				center, radius, verticalRadius, durationTicks, createdAt);
	}

	public int displayIndex() {
		return slot + 1;
	}

	public boolean covers(ResourceKey<Level> candidateDimension, BlockPos position) {
		if (!dimension.equals(candidateDimension)) {
			return false;
		}
		return horizontalDistanceSquared(position) <= (double) radius * radius
				&& Math.abs(position.getY() - center.getY()) <= verticalRadius;
	}

	public double horizontalDistanceSquared(BlockPos position) {
		double dx = position.getX() - center.getX();
		double dz = position.getZ() - center.getZ();
		return dx * dx + dz * dz;
	}

	public double distanceSquaredTo(BlockPos position) {
		double dx = position.getX() - center.getX();
		double dy = position.getY() - center.getY();
		double dz = position.getZ() - center.getZ();
		return dx * dx + dy * dy + dz * dz;
	}
}
