package dev.fimkov.echoesbeneath.memory;

import dev.fimkov.echoesbeneath.memory.snapshot.BlockSnapshot;
import dev.fimkov.echoesbeneath.memory.timeline.BlockTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.EntityTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.ParticleTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.SoundTimeline;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
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
		ParticleTimeline particleTimeline,
		List<UUID> recordedPlayers,
		int recordedVision) {

	public static final int VISION_UNKNOWN = -1;

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
		tag.putInt("recorded_vision", recordedVision);
		tag.put("snapshot", snapshot.toNbt());
		tag.put("block_timeline", blockTimeline.toNbt());
		tag.put("entity_timeline", entityTimeline.toNbt());
		tag.put("sound_timeline", soundTimeline.toNbt());
		tag.put("particle_timeline", particleTimeline.toNbt());
		ListTag players = new ListTag();

		for (UUID player : recordedPlayers) {
			players.add(NbtUtils.createUUID(player));
		}

		tag.put("recorded_players", players);

		return tag;
	}

	public static EchoMemoryRecord fromNbt(CompoundTag tag, HolderGetter<Block> blockLookup,
			int maxBlockEvents, int maxEntityTracks, int maxSoundEvents, int maxParticleEvents) {
		int formatVersion = tag.getIntOr("format_version", 0);

		if (!EchoFormat.isSupported(formatVersion)) {
			throw new EchoMemoryFormatException("Unsupported memory format version " + formatVersion);
		}

		UUID id = tag.contains("id") ? NbtUtils.loadUUID(tag.get("id")) : null;

		if (id == null) {
			throw new EchoMemoryFormatException("Memory record has no identity");
		}

		Identifier dimensionId = Identifier.tryParse(tag.getStringOr("dimension", ""));

		if (dimensionId == null) {
			throw new EchoMemoryFormatException("Memory record has an unreadable dimension");
		}

		int slot = tag.getIntOr("slot", 0);

		if (slot < 0 || slot >= EchoFormat.MAX_MEMORY_SLOTS) {
			throw new EchoMemoryFormatException("Memory record has an out of range slot " + slot);
		}

		BlockPos center = new BlockPos(tag.getIntOr("center_x", 0), tag.getIntOr("center_y", 0), tag.getIntOr("center_z", 0));

		return new EchoMemoryRecord(
				id,
				slot,
				formatVersion,
				ResourceKey.create(Registries.DIMENSION, dimensionId),
				center,
				tag.getIntOr("radius", 0),
				tag.getIntOr("vertical_radius", 0),
				tag.getLongOr("created_at", 0L),
				tag.getIntOr("duration_ticks", 0),
				BlockSnapshot.fromNbt(tag.getCompoundOrEmpty("snapshot"), blockLookup),
				BlockTimeline.fromNbt(tag.getCompoundOrEmpty("block_timeline"), maxBlockEvents),
				EntityTimeline.fromNbt(tag.getCompoundOrEmpty("entity_timeline"), maxEntityTracks),
				SoundTimeline.fromNbt(tag.getCompoundOrEmpty("sound_timeline"), maxSoundEvents),
				tag.contains("particle_timeline")
						? ParticleTimeline.fromNbt(tag.getCompoundOrEmpty("particle_timeline"), maxParticleEvents)
						: new ParticleTimeline(maxParticleEvents),
				readPlayers(tag),
				tag.contains("recorded_vision") ? tag.getIntOr("recorded_vision", 0) : VISION_UNKNOWN);
	}

	private static List<UUID> readPlayers(CompoundTag tag) {
		List<UUID> players = new ArrayList<>();

		for (Tag element : tag.getListOrEmpty("recorded_players")) {
			players.add(NbtUtils.loadUUID(element));
		}

		return List.copyOf(players);
	}

	public boolean holdsPlayerOtherThan(UUID viewer) {
		for (UUID player : recordedPlayers) {
			if (!player.equals(viewer)) {
				return true;
			}
		}

		return false;
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
