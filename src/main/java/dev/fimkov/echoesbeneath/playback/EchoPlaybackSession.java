package dev.fimkov.echoesbeneath.playback;

import dev.fimkov.echoesbeneath.entity.EchoLensEntity;
import dev.fimkov.echoesbeneath.memory.EchoMemoryRecord;
import dev.fimkov.echoesbeneath.network.EchoPlaybackDataPart;
import dev.fimkov.echoesbeneath.network.PlaybackStartPayload;
import dev.fimkov.echoesbeneath.network.PlaybackStopPayload;
import dev.fimkov.echoesbeneath.network.PlaybackTickPayload;
import dev.fimkov.echoesbeneath.playback.suppression.HostileMobSuppressionManager;
import dev.fimkov.echoesbeneath.playback.suppression.SuppressedMobState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EchoPlaybackSession {
	private static final int TICK_SYNC_INTERVAL = 20;
	private static final int SUPPRESSION_SWEEP_INTERVAL = 20;

	private final UUID playbackId;
	private final UUID ownerId;
	private final ServerLevel level;
	private final EchoRegion region;
	private final EchoMemoryRecord record;
	private final EchoLensEntity lensEntity;
	private final Map<EchoPlaybackDataPart, List<byte[]>> encodedParts;
	private final Map<UUID, SuppressedMobState> suppressed = new HashMap<>();
	private final Set<UUID> subscribers = new HashSet<>();

	private int currentTick;
	private boolean finished;

	public EchoPlaybackSession(UUID playbackId, UUID ownerId, ServerLevel level, EchoRegion region,
			EchoMemoryRecord record, EchoLensEntity lensEntity,
			Map<EchoPlaybackDataPart, List<byte[]>> encodedParts) {
		this.playbackId = playbackId;
		this.ownerId = ownerId;
		this.level = level;
		this.region = region;
		this.record = record;
		this.lensEntity = lensEntity;
		this.encodedParts = encodedParts;
	}

	public UUID playbackId() {
		return playbackId;
	}

	public UUID ownerId() {
		return ownerId;
	}

	public ServerLevel level() {
		return level;
	}

	public EchoRegion region() {
		return region;
	}

	public EchoMemoryRecord record() {
		return record;
	}

	public EchoLensEntity lensEntity() {
		return lensEntity;
	}

	public int currentTick() {
		return currentTick;
	}

	public boolean isFinished() {
		return finished;
	}

	public Map<UUID, SuppressedMobState> suppressedStates() {
		return Map.copyOf(suppressed);
	}

	public int suppressedCount() {
		return suppressed.size();
	}

	public void rememberSuppressed(Map<UUID, SuppressedMobState> states) {
		suppressed.putAll(states);
	}

	public boolean hasReachedEnd() {
		return currentTick >= record.durationTicks();
	}

	public void tick(HostileMobSuppressionManager suppressionManager) {
		if (finished || lensEntity.isArriving()) {
			return;
		}
		currentTick++;
		lensEntity.setProgress((float) currentTick / Math.max(1, record.durationTicks()));
		updateSubscribers();
		if (currentTick % TICK_SYNC_INTERVAL == 0) {
			broadcastTick();
		}
		if (currentTick % SUPPRESSION_SWEEP_INTERVAL == 0) {
			sweepSuppression(suppressionManager);
		}
		if (currentTick % PlaybackBoundaryTrace.BEAT_INTERVAL_TICKS == 0) {
			PlaybackBoundaryTrace.sweep(level, region, currentTick);
		}
	}

	private void sweepSuppression(HostileMobSuppressionManager suppressionManager) {
		List<Entity> candidates = level.getEntities((Entity) null, region.bounds(),
				entity -> HostileMobSuppressionManager.isSuppressible(entity)
						&& region.contains(level.dimension(), entity.position()));
		for (Entity entity : candidates) {
			suppressionManager.suppress(entity).ifPresent(state -> suppressed.put(state.entityId(), state));
		}
	}

	private void updateSubscribers() {
		for (ServerPlayer player : level.players()) {
			boolean inside = region.contains(level.dimension(), player.position());
			boolean known = subscribers.contains(player.getUUID());
			if (inside && !known) {
				subscribe(player);
			} else if (!inside && known) {
				unsubscribe(player);
			}
		}
		subscribers.removeIf(id -> level.getServer().getPlayerList().getPlayer(id) == null);
	}

	public void subscribe(ServerPlayer player) {
		subscribers.add(player.getUUID());
		ServerPlayNetworking.send(player, new PlaybackStartPayload(playbackId, region.dimension(),
				region.center(), region.radius(), region.verticalRadius(), record.durationTicks(), record.slot()));
		EchoPlaybackTransfer.sendTo(player, playbackId, encodedParts);
		ServerPlayNetworking.send(player, new PlaybackTickPayload(playbackId, currentTick));
	}

	private void unsubscribe(ServerPlayer player) {
		subscribers.remove(player.getUUID());
		ServerPlayNetworking.send(player, new PlaybackStopPayload(playbackId));
	}

	private void broadcastTick() {
		for (UUID subscriberId : Set.copyOf(subscribers)) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(subscriberId);
			if (player != null) {
				ServerPlayNetworking.send(player, new PlaybackTickPayload(playbackId, currentTick));
			}
		}
	}

	public void broadcastStop() {
		for (UUID subscriberId : Set.copyOf(subscribers)) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(subscriberId);
			if (player != null) {
				ServerPlayNetworking.send(player, new PlaybackStopPayload(playbackId));
			}
		}
		subscribers.clear();
	}

	public void markFinished() {
		finished = true;
	}
}
