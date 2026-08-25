package dev.fimkov.echoesbeneath.client.playback;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.network.EchoPlaybackDataPart;
import dev.fimkov.echoesbeneath.network.PlaybackDataPayload;
import dev.fimkov.echoesbeneath.network.PlaybackStartPayload;
import dev.fimkov.echoesbeneath.network.PlaybackStopPayload;
import dev.fimkov.echoesbeneath.network.PlaybackTickPayload;
import dev.fimkov.echoesbeneath.playback.EchoRegion;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ClientPlaybackManager {
	private static final ClientPlaybackManager INSTANCE = new ClientPlaybackManager();

	private final Map<UUID, ClientPlaybackSession> sessions = new HashMap<>();
	private final Map<UUID, PendingTransfer> transfers = new HashMap<>();

	private ClientPlaybackManager() {
	}

	public static ClientPlaybackManager instance() {
		return INSTANCE;
	}

	public List<ClientPlaybackSession> sessions() {
		return List.copyOf(sessions.values());
	}

	public Optional<ClientPlaybackSession> sessionContaining(ClientLevel level, Vec3 position) {
		for (ClientPlaybackSession session : sessions.values()) {
			if (session.region().contains(level.dimension(), position)) {
				return Optional.of(session);
			}
		}
		return Optional.empty();
	}

	public void onStart(PlaybackStartPayload payload) {
		EchoRegion region = new EchoRegion(payload.dimension(), payload.center(),
				payload.radius(), payload.verticalRadius());
		transfers.put(payload.playbackId(), new PendingTransfer(region, payload.durationTicks(),
				payload.memorySlot(), payload.clarity(), new PlaybackDataAssembler()));
	}

	public void onData(PlaybackDataPayload payload, ClientLevel level) {
		PendingTransfer transfer = transfers.get(payload.playbackId());
		if (transfer == null) {
			return;
		}
		try {
			transfer.assembler().accept(payload.part(), payload.chunkIndex(), payload.chunkCount(), payload.chunk());
			if (transfer.assembler().isComplete()) {
				finishTransfer(payload.playbackId(), transfer, level);
			}
		} catch (RuntimeException failure) {
			EchoesBeneath.LOGGER.error("Discarding playback {} after a malformed transfer", payload.playbackId(), failure);
			transfers.remove(payload.playbackId());
		}
	}

	private void finishTransfer(UUID playbackId, PendingTransfer transfer, ClientLevel level) {
		transfers.remove(playbackId);
		try {
			var blockLookup = level.registryAccess().<Block>registryOrThrow(Registries.BLOCK).asLookup();
			ClientPlaybackData data = ClientPlaybackData.assemble(
					transfer.assembler().decode(EchoPlaybackDataPart.SNAPSHOT),
					transfer.assembler().decode(EchoPlaybackDataPart.BLOCK_TIMELINE),
					transfer.assembler().decode(EchoPlaybackDataPart.ENTITY_TIMELINE),
					transfer.assembler().decode(EchoPlaybackDataPart.SOUND_TIMELINE),
					blockLookup);
			sessions.put(playbackId, new ClientPlaybackSession(playbackId, transfer.region(),
					transfer.durationTicks(), transfer.memorySlot(), transfer.clarity(), data));
		} catch (Exception failure) {
			EchoesBeneath.LOGGER.error("Unable to decode playback {}", playbackId, failure);
		}
	}

	public void onTick(PlaybackTickPayload payload) {
		ClientPlaybackSession session = sessions.get(payload.playbackId());
		if (session != null) {
			session.syncTo(payload.currentTick());
		}
	}

	public void onStop(PlaybackStopPayload payload) {
		transfers.remove(payload.playbackId());
		ClientPlaybackSession session = sessions.remove(payload.playbackId());
		if (session != null) {
			session.discard();
		}
	}

	public void clientTick(ClientLevel level) {
		for (ClientPlaybackSession session : List.copyOf(sessions.values())) {
			session.clientTick(level);
		}
	}

	public void clearAll() {
		for (ClientPlaybackSession session : sessions.values()) {
			session.discard();
		}
		sessions.clear();
		transfers.clear();
	}

	private record PendingTransfer(EchoRegion region, int durationTicks, int memorySlot, int clarity,
			PlaybackDataAssembler assembler) {
	}
}
