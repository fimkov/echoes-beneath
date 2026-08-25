package dev.fimkov.echoesbeneath.memory;

import dev.fimkov.echoesbeneath.memory.snapshot.BlockSnapshot;
import dev.fimkov.echoesbeneath.memory.timeline.BlockTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.EntityTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.ParticleTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.SoundTimeline;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.UUID;

public record EchoMemoryRecord(
		UUID id,
		int slot,
		int formatVersion,
		ResourceKey<Level> dimension,
		BlockPos center,
		int radius,
		int verticalRadius,
		long createdAtEpochMillis,
		int durationTicks,
		BlockSnapshot snapshot,
		BlockTimeline blockTimeline,
		EntityTimeline entityTimeline,
		SoundTimeline soundTimeline,
		ParticleTimeline particleTimeline) {

	public EchoMemoryReference toReference() {
		return new EchoMemoryReference(id, slot, dimension, center, radius, verticalRadius,
				durationTicks, createdAtEpochMillis);
	}

	public CompoundTag toNbt() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("format_version", formatVersion);
		tag.put("id", NbtUtils.createUUID(id));
		tag.putInt("slot", slot);
		tag.putString("dimension", dimension.location().toString());
		tag.putInt("center_x", center.getX());
		tag.putInt("center_y", center.getY());
		tag.putInt("center_z", center.getZ());
		tag.putInt("radius", radius);
		tag.putInt("vertical_radius", verticalRadius);
		tag.putLong("created_at", createdAtEpochMillis);
		tag.putInt("duration_ticks", durationTicks);
		tag.put("snapshot", snapshot.toNbt());
		tag.put("block_timeline", blockTimeline.toNbt());
		tag.put("entity_timeline", entityTimeline.toNbt());
		tag.put("sound_timeline", soundTimeline.toNbt());
		tag.put("particle_timeline", particleTimeline.toNbt());
		return tag;
	}

	public static EchoMemoryRecord fromNbt(CompoundTag tag, HolderGetter<Block> blockLookup,
			int maxBlockEvents, int maxEntityTracks, int maxSoundEvents, int maxParticleEvents) {
		int formatVersion = tag.getInt("format_version");
		if (!EchoFormat.isSupported(formatVersion)) {
			throw new EchoMemoryFormatException("Unsupported memory format version " + formatVersion);
		}
		UUID id = tag.contains("id") ? NbtUtils.loadUUID(tag.get("id")) : null;
		if (id == null) {
			throw new EchoMemoryFormatException("Memory record has no identity");
		}
		ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("dimension"));
		if (dimensionId == null) {
			throw new EchoMemoryFormatException("Memory record has an unreadable dimension");
		}
		int slot = tag.getInt("slot");
		if (slot < 0 || slot >= EchoFormat.MAX_MEMORY_SLOTS) {
			throw new EchoMemoryFormatException("Memory record has an out of range slot " + slot);
		}
		BlockPos center = new BlockPos(tag.getInt("center_x"), tag.getInt("center_y"), tag.getInt("center_z"));
		return new EchoMemoryRecord(
				id,
				slot,
				formatVersion,
				ResourceKey.create(Registries.DIMENSION, dimensionId),
				center,
				tag.getInt("radius"),
				tag.getInt("vertical_radius"),
				tag.getLong("created_at"),
				tag.getInt("duration_ticks"),
				BlockSnapshot.fromNbt(tag.getCompound("snapshot"), blockLookup),
				BlockTimeline.fromNbt(tag.getCompound("block_timeline"), maxBlockEvents),
				EntityTimeline.fromNbt(tag.getCompound("entity_timeline"), maxEntityTracks),
				SoundTimeline.fromNbt(tag.getCompound("sound_timeline"), maxSoundEvents),
				tag.contains("particle_timeline")
						? ParticleTimeline.fromNbt(tag.getCompound("particle_timeline"), maxParticleEvents)
						: new ParticleTimeline(maxParticleEvents));
	}

	public void validate() {
		if (!EchoFormat.isSupported(formatVersion)) {
			throw new EchoMemoryFormatException("Unsupported memory format version " + formatVersion);
		}
		if (radius <= 0 || verticalRadius <= 0) {
			throw new EchoMemoryFormatException("Memory record has a degenerate region");
		}
		if (durationTicks <= 0) {
			throw new EchoMemoryFormatException("Memory record has no duration");
		}
		if (!EchoCoordinates.fitsInPackedRange(radius) || !EchoCoordinates.fitsInPackedRange(verticalRadius)) {
			throw new EchoMemoryFormatException("Memory record exceeds the packed coordinate range");
		}
	}
}
