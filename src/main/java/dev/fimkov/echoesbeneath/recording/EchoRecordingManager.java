package dev.fimkov.echoesbeneath.recording;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.config.EchoConfig;
import dev.fimkov.echoesbeneath.advancement.EchoCriteria;
import dev.fimkov.echoesbeneath.entity.EchoLensEntity;
import dev.fimkov.echoesbeneath.entity.EchoLensMode;
import dev.fimkov.echoesbeneath.item.EchoLensStacks;
import dev.fimkov.echoesbeneath.memory.timeline.SoundTimeline;
import dev.fimkov.echoesbeneath.memory.EchoMemoryRecord;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReferences;
import dev.fimkov.echoesbeneath.memory.EchoTextFormat;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import dev.fimkov.echoesbeneath.runtime.EchoLensReturn;
import dev.fimkov.echoesbeneath.runtime.EchoServerState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

public final class EchoRecordingManager {
	private static final int COMPLETION_PARTICLE_COUNT = 40;
	private static final double COMPLETION_PARTICLE_SPREAD = 1.5D;
	private static final float COMPLETION_VOLUME = 0.9F;
	private static final float COMPLETION_PITCH = 1.2F;

	private final EchoServerState state;
	private final Map<UUID, EchoRecordingSession> sessions = new HashMap<>();

	public EchoRecordingManager(EchoServerState state) {
		this.state = state;
	}

	public Collection<EchoRecordingSession> allSessions() {
		return List.copyOf(sessions.values());
	}

	public List<EchoRecordingSession> sessionsIn(ServerLevel level) {
		List<EchoRecordingSession> matching = new ArrayList<>(sessions.size());
		for (EchoRecordingSession session : sessions.values()) {
			if (session.level() == level && !session.isFinished()) {
				matching.add(session);
			}
		}
		return matching;
	}

	public Optional<EchoRecordingSession> session(UUID sessionId) {
		return Optional.ofNullable(sessions.get(sessionId));
	}

	public Optional<EchoRecordingSession> sessionOwnedBy(UUID ownerId) {
		for (EchoRecordingSession session : sessions.values()) {
			if (session.ownerId().equals(ownerId)) {
				return Optional.of(session);
			}
		}
		return Optional.empty();
	}

	public Optional<EchoRecordingSession> start(ServerPlayer player, InteractionHand hand, BlockPos supportBlock) {
		ItemStack lensStack = player.getItemInHand(hand);
		EchoMemoryReferences references = EchoLensStacks.references(lensStack);
		Optional<Integer> freeSlot = references.nextFreeSlot(EchoLensStacks.upgrades(lensStack).memorySlots());
		if (freeSlot.isEmpty()) {
			player.displayClientMessage(Component.translatable(EchoTranslations.MESSAGE_NO_MORE), true);
			return Optional.empty();
		}
		ServerLevel level = player.serverLevel();
		BlockPos center = supportBlock.above();
		Vec3 hoverPosition = new Vec3(center.getX() + 0.5D,
				center.getY() + EchoLensEntity.HOVER_HEIGHT, center.getZ() + 0.5D);

		ItemStack deployed = lensStack.copy();
		EchoLensStacks.identityOrAssign(deployed);
		UUID sessionId = UUID.randomUUID();

		Vec3 launchPosition = player.getEyePosition().add(player.getLookAngle().scale(0.6D));
		EchoLensEntity lensEntity = new EchoLensEntity(level, launchPosition, deployed,
				player.getUUID(), sessionId, EchoLensMode.RECORDING);
		lensEntity.beginArrival(launchPosition, hoverPosition);
		if (!level.addFreshEntity(lensEntity)) {
			return Optional.empty();
		}
		player.setItemInHand(hand, ItemStack.EMPTY);

		EchoRecordingSession session = new EchoRecordingSession(sessionId, UUID.randomUUID(), player.getUUID(),
				freeSlot.get(), level, center, EchoConfig.values(), EchoLensStacks.upgrades(deployed),
				lensEntity, System.currentTimeMillis());
		sessions.put(sessionId, session);
		EchoRecordingHooks.setAnyRecordingActive(true);
		return Optional.of(session);
	}

	public void tick() {
		if (sessions.isEmpty()) {
			return;
		}
		for (EchoRecordingSession session : List.copyOf(sessions.values())) {
			tickSession(session);
		}
		EchoRecordingHooks.setAnyRecordingActive(!sessions.isEmpty());
	}

	private void tickSession(EchoRecordingSession session) {
		EchoLensEntity lens = session.lensEntity();
		if (lens.isRemoved()) {
			stop(session, EchoRecordingStopReason.LENS_LOST);
			return;
		}
		if (!session.regionStillLoaded()) {
			stop(session, EchoRecordingStopReason.CHUNKS_UNLOADED);
			return;
		}
		try {
			session.tick();
		} catch (RuntimeException failure) {
			EchoesBeneath.LOGGER.error("Recording {} failed during tick", session.sessionId(), failure);
			stop(session, EchoRecordingStopReason.FAILURE);
			return;
		}
		if (session.hasReachedDuration()) {
			stop(session, EchoRecordingStopReason.DURATION_REACHED);
		}
	}

	public void stopAll(EchoRecordingStopReason reason) {
		for (EchoRecordingSession session : List.copyOf(sessions.values())) {
			stop(session, reason);
		}
	}

	public void stopIn(ServerLevel level, EchoRecordingStopReason reason) {
		for (EchoRecordingSession session : List.copyOf(sessions.values())) {
			if (session.level() == level) {
				stop(session, reason);
			}
		}
	}

	public void stop(EchoRecordingSession session, EchoRecordingStopReason reason) {
		sessions.remove(session.sessionId());
		EchoRecordingHooks.setAnyRecordingActive(!sessions.isEmpty());
		if (session.isFinished()) {
			return;
		}
		if (!reason.sealsMemory()) {
			session.markFinished();
			releaseWithoutSealing(session);
			return;
		}
		try {
			sealAndReturn(session, reason);
		} catch (RuntimeException failure) {
			EchoesBeneath.LOGGER.error("Unable to seal memory for recording {}", session.sessionId(), failure);
			session.markFinished();
			releaseWithoutSealing(session);
		}
	}

	private void sealAndReturn(EchoRecordingSession session, EchoRecordingStopReason reason) {
		EchoMemoryRecord record = session.seal();
		if (!state.storage().save(record)) {
			releaseWithoutSealing(session);
			return;
		}
		EchoLensEntity lens = session.lensEntity();
		ItemStack stack = lens.getLens();
		if (!stack.isEmpty()) {
			EchoLensStacks.setReferences(stack, EchoLensStacks.references(stack).withAdded(record.toReference()));
			lens.setLens(stack);
		}
		awardRecording(session, record, stack);
		playCompletionEffects(session);
		notifyOwner(session, reason);
		lens.setMode(EchoLensMode.RETURNING);
	}

	private void awardRecording(EchoRecordingSession session, EchoMemoryRecord record, ItemStack stack) {
		ServerPlayer owner = session.level().getServer().getPlayerList().getPlayer(session.ownerId());
		if (owner == null) {
			return;
		}
		SoundTimeline sounds = record.soundTimeline();
		Set<ResourceLocation> heard = new HashSet<>();
		for (int index = 0; index < sounds.size(); index++) {
			heard.add(sounds.soundAt(index));
		}
		EchoCriteria.MEMORY_RECORDED.trigger(owner, sounds.size(),
				EchoLensStacks.references(stack).filledSlots(), record.durationTicks(), heard);
	}

	public void handOverTo(ServerPlayer owner) {
		for (EchoRecordingSession session : List.copyOf(sessions.values())) {
			if (!session.ownerId().equals(owner.getUUID())) {
				continue;
			}
			stop(session, EchoRecordingStopReason.OWNER_LEFT);
			EchoLensEntity lens = session.lensEntity();
			if (!lens.isRemoved()) {
				EchoLensReturn.deliverTo(lens, owner);
			}
		}
	}

	private void releaseWithoutSealing(EchoRecordingSession session) {
		EchoLensEntity lens = session.lensEntity();
		if (!lens.isRemoved()) {
			lens.setMode(EchoLensMode.ABANDONED);
		}
	}

	private void playCompletionEffects(EchoRecordingSession session) {
		ServerLevel level = session.level();
		EchoLensEntity lens = session.lensEntity();
		EchoRecordingHooks.runWithoutCapture(() -> {
			level.playSound(null, lens.getX(), lens.getY(), lens.getZ(),
					EchoSounds.LENS_RECORDING_COMPLETE, SoundSource.BLOCKS, COMPLETION_VOLUME, COMPLETION_PITCH);
			level.sendParticles(ParticleTypes.SCULK_SOUL, lens.getX(), lens.getY(), lens.getZ(),
					COMPLETION_PARTICLE_COUNT, COMPLETION_PARTICLE_SPREAD, COMPLETION_PARTICLE_SPREAD,
					COMPLETION_PARTICLE_SPREAD, 0.0D);
		});
	}

	private void notifyOwner(EchoRecordingSession session, EchoRecordingStopReason reason) {
		if (reason != EchoRecordingStopReason.CHUNKS_UNLOADED) {
			return;
		}
		ServerPlayer owner = session.level().getServer().getPlayerList().getPlayer(session.ownerId());
		if (owner != null) {
			owner.displayClientMessage(Component.translatable(
					EchoTranslations.MESSAGE_RECORDING_STOPPED_UNLOADED,
					EchoTextFormat.romanSlot(session.slot())), true);
		}
	}

	public boolean requestEarlyStop(UUID sessionId, UUID requesterId) {
		EchoRecordingSession session = sessions.get(sessionId);
		if (session == null || !session.ownerId().equals(requesterId)) {
			return false;
		}
		stop(session, EchoRecordingStopReason.OWNER_REQUEST);
		return true;
	}
}
