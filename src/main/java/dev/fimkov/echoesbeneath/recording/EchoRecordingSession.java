package dev.fimkov.echoesbeneath.recording;

import dev.fimkov.echoesbeneath.config.EchoConfigValues;
import dev.fimkov.echoesbeneath.entity.EchoLensEntity;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import dev.fimkov.echoesbeneath.memory.EchoFormat;
import dev.fimkov.echoesbeneath.memory.EchoMemoryRecord;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockPalette;
import dev.fimkov.echoesbeneath.memory.timeline.BlockTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.EchoSoundCategory;
import dev.fimkov.echoesbeneath.memory.timeline.EntityTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.ParticleTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.SoundTimeline;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class EchoRecordingSession {
	private static final int MAX_BLOCK_EVENTS = 65536;
	private static final float QUIET_SOUND_THRESHOLD = 0.12F;

	private final UUID sessionId;
	private final UUID memoryId;
	private final UUID ownerId;
	private final int slot;
	private final ServerLevel level;
	private final BlockPos center;
	private final int radius;
	private final int verticalRadius;
	private final int maxTicks;
	private final long createdAtEpochMillis;
	private final int entitySampleInterval;

	private final BlockPalette palette = new BlockPalette();
	private final SnapshotCapture snapshotCapture;
	private final BlockTimeline blockTimeline;
	private final EntityTimeline entityTimeline;
	private final SoundTimeline soundTimeline;
	private final ParticleTimeline particleTimeline;
	private static final int FIRST_BREATH_TICK = 1;
	private static final int LISTEN_PULSE_TICKS = 60;
	private static final float BREATH_VOLUME = 0.8F;
	private static final float BREATH_PITCH = 1.0F;
	private static final float PULSE_VOLUME = 0.28F;
	private static final float PULSE_PITCH = 1.0F;
	private final Set<UUID> recordedPlayers = new LinkedHashSet<>();
	private final EntityTrackRecorder entityRecorder;
	private final EchoLensEntity lensEntity;

	private final float sensitivityFactor;
	private final int vision;
	private int currentTick;
	private boolean finished;

	public EchoRecordingSession(UUID sessionId, UUID memoryId, UUID ownerId, int slot, ServerLevel level,
			BlockPos center, EchoConfigValues config, EchoLensUpgrades upgrades,
			EchoLensEntity lensEntity, long createdAtEpochMillis) {
		this.sessionId = sessionId;
		this.memoryId = memoryId;
		this.ownerId = ownerId;
		this.slot = slot;
		this.level = level;
		this.center = center;
		this.radius = Math.min(config.recordingRadius(), upgrades.radius());
		this.verticalRadius = config.recordingVerticalRadius();
		this.maxTicks = config.maxRecordingTicks();
		this.entitySampleInterval = config.entitySampleIntervalTicks();
		this.createdAtEpochMillis = createdAtEpochMillis;
		this.lensEntity = lensEntity;
		this.snapshotCapture = new SnapshotCapture(level, center, radius, verticalRadius, palette);
		this.blockTimeline = new BlockTimeline(MAX_BLOCK_EVENTS);
		this.entityTimeline = new EntityTimeline(config.maxRecordedEntities());
		this.soundTimeline = new SoundTimeline(config.maxSoundEvents());
		this.particleTimeline = new ParticleTimeline(config.maxParticleEvents());
		this.sensitivityFactor = upgrades.sensitivityFactor();
		this.vision = upgrades.vision();
		this.entityRecorder = new EntityTrackRecorder(entityTimeline, center);
	}

	public UUID sessionId() {
		return sessionId;
	}

	public UUID memoryId() {
		return memoryId;
	}

	public UUID ownerId() {
		return ownerId;
	}

	public int slot() {
		return slot;
	}

	public ServerLevel level() {
		return level;
	}

	public BlockPos center() {
		return center;
	}

	public int currentTick() {
		return currentTick;
	}

	public int maxTicks() {
		return maxTicks;
	}

	public boolean isFinished() {
		return finished;
	}

	public EchoLensEntity lensEntity() {
		return lensEntity;
	}

	public int recordedSoundCount() {
		return soundTimeline.size();
	}

	public int recordedEntityCount() {
		return entityRecorder.trackedCount();
	}

	public int recordedBlockEventCount() {
		return blockTimeline.size();
	}

	public boolean containsPosition(BlockPos position) {
		int relativeX = position.getX() - center.getX();
		int relativeY = position.getY() - center.getY();
		int relativeZ = position.getZ() - center.getZ();

		return withinRegion(relativeX, relativeY, relativeZ);
	}

	private boolean withinRegion(double relativeX, double relativeY, double relativeZ) {
		return relativeX * relativeX + relativeZ * relativeZ <= (double) radius * radius
				&& Math.abs(relativeY) <= verticalRadius;
	}

	public void onBlockChanged(BlockPos position, BlockState previous, BlockState current) {
		if (finished || previous == current) {
			return;
		}

		int relativeX = position.getX() - center.getX();
		int relativeY = position.getY() - center.getY();
		int relativeZ = position.getZ() - center.getZ();

		if (!withinRegion(relativeX, relativeY, relativeZ)) {
			return;
		}

		if (!EchoCoordinates.fitsInPackedRange(relativeX) || !EchoCoordinates.fitsInPackedRange(relativeY)

				|| !EchoCoordinates.fitsInPackedRange(relativeZ)) {
			return;
		}

		snapshotCapture.notePreChange(relativeX, relativeY, relativeZ, previous);
		blockTimeline.record(currentTick, EchoCoordinates.packRelative(relativeX, relativeY, relativeZ),
				palette.indexOf(previous), palette.indexOf(current));
	}

	public void onSound(double x, double y, double z, Identifier soundId, SoundSource source,
			float volume, float pitch) {
		if (finished || !EchoSoundCategory.isRecordable(source)) {
			return;
		}

		double relativeX = x - center.getX();
		double relativeY = y - center.getY();
		double relativeZ = z - center.getZ();

		if (!withinRegion(relativeX, relativeY, relativeZ)) {
			return;
		}

		if (volume < sensitivityFactor * QUIET_SOUND_THRESHOLD) {
			return;
		}

		soundTimeline.record(currentTick, relativeX, relativeY, relativeZ, soundId, source, volume, pitch);
	}

	public void onParticle(double x, double y, double z, Identifier typeId, int count,
			double spreadX, double spreadY, double spreadZ) {
		if (finished) {
			return;
		}

		double relativeX = x - center.getX();
		double relativeY = y - center.getY();
		double relativeZ = z - center.getZ();

		if (!withinRegion(relativeX, relativeY, relativeZ)) {
			return;
		}

		particleTimeline.record(currentTick, relativeX, relativeY, relativeZ, typeId, count,
				spreadX, spreadY, spreadZ);
	}

	public void tick() {
		if (finished || lensEntity.isArriving()) {
			return;
		}

		if (!snapshotCapture.isComplete()) {
			snapshotCapture.advance();
		}

		if (currentTick % entitySampleInterval == 0) {
			sampleEntities();
		}

		lensEntity.setProgress((float) currentTick / Math.max(1, maxTicks));

		if (currentTick == FIRST_BREATH_TICK) {
			EchoRecordingHooks.runWithoutCapture(() -> level.playSound(null, center,
					EchoSounds.LENS_RECORDING_START, SoundSource.AMBIENT,
					BREATH_VOLUME, BREATH_PITCH));
		}

		if (currentTick > 0 && currentTick % LISTEN_PULSE_TICKS == 0) {
			EchoRecordingHooks.runWithoutCapture(() -> level.playSound(null, center,
					EchoSounds.LENS_RECORDING_PULSE, SoundSource.AMBIENT,
					PULSE_VOLUME, PULSE_PITCH));
		}

		if (currentTick % RecordingPulseTrace.BEAT_INTERVAL_TICKS == 0) {
			RecordingPulseTrace.sweep(level, level.getServer().getPlayerList().getPlayer(ownerId),
					center, radius, verticalRadius, currentTick);
		}

		currentTick++;
	}

	private void sampleEntities() {
		AABB bounds = new AABB(
				center.getX() - radius, center.getY() - verticalRadius, center.getZ() - radius,
				center.getX() + radius + 1, center.getY() + verticalRadius + 1, center.getZ() + radius + 1);
		List<Entity> candidates = level.getEntities((Entity) null, bounds, EntityTrackRecorder::isRecordable);

		for (Entity entity : candidates) {
			if (!containsPosition(entity.blockPosition())) {
				continue;
			}
			if (entity instanceof Player) {
				recordedPlayers.add(entity.getUUID());
			}
			entityRecorder.sample(entity, currentTick);
		}
	}

	public boolean hasReachedDuration() {
		return currentTick >= maxTicks;
	}

	public boolean regionStillLoaded() {
		return level.hasChunkAt(center);
	}

	public EchoMemoryRecord seal() {
		finished = true;
		snapshotCapture.completeNow();

		return new EchoMemoryRecord(
				memoryId,
				slot,
				EchoFormat.CURRENT_VERSION,
				level.dimension(),
				center,
				radius,
				verticalRadius,
				createdAtEpochMillis,
				Math.max(1, currentTick),
				snapshotCapture.toSnapshot(),
				blockTimeline,
				entityTimeline,
				soundTimeline,
				particleTimeline,
				List.copyOf(recordedPlayers),
				vision);
	}

	public void markFinished() {
		finished = true;
	}
}
