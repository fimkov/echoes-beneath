package dev.fimkov.echoesbeneath.playback;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.config.EchoConfig;
import dev.fimkov.echoesbeneath.entity.EchoLensEntity;
import dev.fimkov.echoesbeneath.entity.EchoLensMode;
import dev.fimkov.echoesbeneath.item.EchoLensStacks;
import dev.fimkov.echoesbeneath.memory.EchoMemoryRecord;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import dev.fimkov.echoesbeneath.memory.EchoTextFormat;
import dev.fimkov.echoesbeneath.network.EchoPlaybackDataPart;
import dev.fimkov.echoesbeneath.recording.EchoRecordingHooks;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import dev.fimkov.echoesbeneath.runtime.EchoServerState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EchoPlaybackManager {
	private static final int HOVER_SEARCH_RADIUS = 3;
	private static final float START_VOLUME = 0.8F;
	private static final float START_PITCH = 0.7F;
	private static final float END_VOLUME = 0.7F;
	private static final float END_PITCH = 1.1F;
	private static final int END_PARTICLE_COUNT = 60;
	private static final double END_PARTICLE_SPREAD = 2.0D;

	private final EchoServerState state;
	private final Map<UUID, EchoPlaybackSession> sessions = new HashMap<>();

	public EchoPlaybackManager(EchoServerState state) {
		this.state = state;
	}

	public Collection<EchoPlaybackSession> allSessions() {
		return List.copyOf(sessions.values());
	}

	public Optional<EchoPlaybackSession> sessionAt(ServerLevel level, Vec3 position) {
		for (EchoPlaybackSession session : sessions.values()) {
			if (session.level() == level && session.region().contains(level.dimension(), position)) {
				return Optional.of(session);
			}
		}
		return Optional.empty();
	}

	public Optional<EchoPlaybackSession> session(UUID playbackId) {
		return Optional.ofNullable(sessions.get(playbackId));
	}

	public Optional<EchoPlaybackSession> sessionOwnedBy(UUID ownerId) {
		for (EchoPlaybackSession session : sessions.values()) {
			if (session.ownerId().equals(ownerId)) {
				return Optional.of(session);
			}
		}
		return Optional.empty();
	}

	public boolean isInsideAnyRegion(ServerLevel level, BlockPos position) {
		for (EchoPlaybackSession session : sessions.values()) {
			if (session.level() == level && session.region().contains(level.dimension(), position)) {
				return true;
			}
		}
		return false;
	}

	public boolean start(ServerPlayer player, InteractionHand hand, EchoMemoryReference reference) {
		ItemStack lensStack = player.getItemInHand(hand);
		Optional<EchoMemoryRecord> loaded = state.storage().load(reference.memoryId());
		if (loaded.isEmpty()) {
			player.displayClientMessage(Component.translatable(EchoTranslations.MESSAGE_MEMORY_CORRUPT), true);
			return false;
		}
		EchoMemoryRecord record = loaded.get();
		ServerLevel level = player.serverLevel();
		EchoRegion region = new EchoRegion(reference.dimension(), reference.center(),
				reference.radius(), reference.verticalRadius());
		if (!EchoConfig.values().allowOverlappingPlayback() && overlapsExisting(region)) {
			player.displayClientMessage(Component.translatable(EchoTranslations.MESSAGE_PLAYBACK_OVERLAP), true);
			return false;
		}

		Map<EchoPlaybackDataPart, List<byte[]>> encoded;
		try {
			encoded = EchoPlaybackTransfer.encode(record);
		} catch (RuntimeException failure) {
			EchoesBeneath.LOGGER.error("Unable to encode memory {} for playback", reference.memoryId(), failure);
			player.displayClientMessage(Component.translatable(EchoTranslations.MESSAGE_MEMORY_CORRUPT), true);
			return false;
		}

		ItemStack deployed = lensStack.copy();
		EchoLensStacks.identityOrAssign(deployed);
		UUID playbackId = UUID.randomUUID();
		Vec3 hover = findHoverPosition(level, region.center());
		Vec3 launchPosition = player.getEyePosition().add(player.getLookAngle().scale(0.6D));
		EchoLensEntity lensEntity = new EchoLensEntity(level, launchPosition, deployed,
				player.getUUID(), playbackId, EchoLensMode.PLAYBACK);
		lensEntity.beginArrival(launchPosition, hover);
		if (!level.addFreshEntity(lensEntity)) {
			return false;
		}
		player.setItemInHand(hand, ItemStack.EMPTY);

		EchoPlaybackSession session = new EchoPlaybackSession(playbackId, player.getUUID(), level,
				region, record, lensEntity, encoded, EchoLensStacks.upgrades(deployed).clarity());
		if (EchoConfig.values().suppressHostileMobs()) {
			session.rememberSuppressed(state.suppression().suppressRegion(level, region));
		}
		sessions.put(playbackId, session);
		EchoRecordingHooks.runWithoutCapture(() -> level.playSound(null,
				launchPosition.x, launchPosition.y, launchPosition.z,
				EchoSounds.PLAYBACK_START, SoundSource.AMBIENT, START_VOLUME, START_PITCH));
		return true;
	}

	private boolean overlapsExisting(EchoRegion region) {
		for (EchoPlaybackSession session : sessions.values()) {
			if (session.region().overlaps(region)) {
				return true;
			}
		}
		return false;
	}

	private Vec3 findHoverPosition(ServerLevel level, BlockPos center) {
		BlockPos preferred = center;
		for (int radius = 0; radius <= HOVER_SEARCH_RADIUS; radius++) {
			for (int offsetY = 0; offsetY <= radius; offsetY++) {
				BlockPos candidate = preferred.offset(0, offsetY, 0);
				if (level.getBlockState(candidate).isAir()) {
					return new Vec3(candidate.getX() + 0.5D,
							candidate.getY() + EchoLensEntity.HOVER_HEIGHT, candidate.getZ() + 0.5D);
				}
			}
		}
		return new Vec3(center.getX() + 0.5D, center.getY() + EchoLensEntity.HOVER_HEIGHT, center.getZ() + 0.5D);
	}

	public void tick() {
		if (sessions.isEmpty()) {
			return;
		}
		for (EchoPlaybackSession session : List.copyOf(sessions.values())) {
			tickSession(session);
		}
	}

	private void tickSession(EchoPlaybackSession session) {
		if (session.lensEntity().isRemoved()) {
			stop(session, EchoPlaybackStopReason.LENS_LOST);
			return;
		}
		try {
			session.tick(state.suppression());
		} catch (RuntimeException failure) {
			EchoesBeneath.LOGGER.error("Playback {} failed during tick", session.playbackId(), failure);
			stop(session, EchoPlaybackStopReason.FAILURE);
			return;
		}
		if (session.hasReachedEnd()) {
			stop(session, EchoPlaybackStopReason.TIMELINE_COMPLETE);
		}
	}

	public void stopAll(EchoPlaybackStopReason reason) {
		for (EchoPlaybackSession session : List.copyOf(sessions.values())) {
			stop(session, reason);
		}
	}

	public void stopIn(ServerLevel level, EchoPlaybackStopReason reason) {
		for (EchoPlaybackSession session : List.copyOf(sessions.values())) {
			if (session.level() == level) {
				stop(session, reason);
			}
		}
	}

	public void stop(EchoPlaybackSession session, EchoPlaybackStopReason reason) {
		sessions.remove(session.playbackId());
		if (session.isFinished()) {
			return;
		}
		session.markFinished();
		state.suppression().restoreAll(session.suppressedStates());
		session.broadcastStop();
		playEndingEffects(session);
		EchoLensEntity lens = session.lensEntity();
		if (!lens.isRemoved()) {
			lens.setMode(EchoLensMode.RETURNING);
		}
		EchoesBeneath.LOGGER.debug("Playback {} stopped because {}", session.playbackId(), reason);
	}

	private void playEndingEffects(EchoPlaybackSession session) {
		ServerLevel level = session.level();
		Vec3 center = session.region().centerVector();
		EchoRecordingHooks.runWithoutCapture(() -> {
			level.playSound(null, center.x, center.y, center.z,
					EchoSounds.PLAYBACK_END, SoundSource.AMBIENT, END_VOLUME, END_PITCH);
			level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, center.x, center.y, center.z,
					END_PARTICLE_COUNT, END_PARTICLE_SPREAD, END_PARTICLE_SPREAD, END_PARTICLE_SPREAD, 0.0D);
		});
	}

	public boolean requestEarlyStop(UUID playbackId, UUID requesterId) {
		EchoPlaybackSession session = sessions.get(playbackId);
		if (session == null || !session.ownerId().equals(requesterId)) {
			return false;
		}
		stop(session, EchoPlaybackStopReason.OWNER_REQUEST);
		return true;
	}

	public void onOwnerDisconnected(UUID ownerId) {
		for (EchoPlaybackSession session : List.copyOf(sessions.values())) {
			if (session.ownerId().equals(ownerId)) {
				stop(session, EchoPlaybackStopReason.OWNER_LEFT);
			}
		}
	}

	public boolean protectsEntity(Entity entity) {
		for (EchoPlaybackSession session : sessions.values()) {
			if (session.suppressedStates().containsKey(entity.getUUID())) {
				return true;
			}
		}
		return false;
	}
}
