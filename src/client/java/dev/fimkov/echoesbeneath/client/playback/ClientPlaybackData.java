package dev.fimkov.echoesbeneath.client.playback;

import dev.fimkov.echoesbeneath.config.EchoConfig;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockSnapshot;
import dev.fimkov.echoesbeneath.memory.timeline.BlockTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.EntityTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.ParticleTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.SoundTimeline;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;

public record ClientPlaybackData(
		BlockSnapshot snapshot,
		BlockTimeline blockTimeline,
		EntityTimeline entityTimeline,
		SoundTimeline soundTimeline,
		ParticleTimeline particleTimeline) {

	private static final int MAX_BLOCK_EVENTS = 65536;

	public static ClientPlaybackData assemble(CompoundTag snapshotTag, CompoundTag blockTag,
			CompoundTag entityTag, CompoundTag soundTag, CompoundTag particleTag,
			HolderGetter<Block> blockLookup) {
		return new ClientPlaybackData(
				BlockSnapshot.fromNbt(snapshotTag, blockLookup),
				BlockTimeline.fromNbt(blockTag, MAX_BLOCK_EVENTS),
				EntityTimeline.fromNbt(entityTag, EchoConfig.values().maxRecordedEntities()),
				SoundTimeline.fromNbt(soundTag, EchoConfig.values().maxSoundEvents()),
				ParticleTimeline.fromNbt(particleTag, EchoConfig.values().maxParticleEvents()));
	}
}
