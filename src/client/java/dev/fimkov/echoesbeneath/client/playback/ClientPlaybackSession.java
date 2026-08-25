package dev.fimkov.echoesbeneath.client.playback;

import dev.fimkov.echoesbeneath.client.render.EchoBlockDeltaState;
import dev.fimkov.echoesbeneath.client.render.EchoEntityPuppet;
import dev.fimkov.echoesbeneath.client.render.EchoSectionMesh;
import dev.fimkov.echoesbeneath.client.sound.EchoSoundPlayer;
import dev.fimkov.echoesbeneath.client.wave.EchoWaveManager;
import dev.fimkov.echoesbeneath.memory.timeline.EntityTrack;
import dev.fimkov.echoesbeneath.playback.EchoRegion;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClientPlaybackSession {
	private static final int MESH_BUILDS_PER_FRAME = 3;

	private final UUID playbackId;
	private final EchoRegion region;
	private final int durationTicks;
	private final int memorySlot;
	private final ClientPlaybackData data;
	private final EchoWaveManager waves = new EchoWaveManager();
	private final EchoSoundPlayer soundPlayer;
	private final EchoBlockDeltaState blockDeltas;
	private final Int2ObjectMap<EchoSectionMesh> meshes = new Int2ObjectOpenHashMap<>();
	private final IntList pendingSectionKeys = new IntArrayList();
	private final List<EchoEntityPuppet> puppets = new ArrayList<>();

	private int currentTick;
	private boolean puppetsInitialised;

	public ClientPlaybackSession(UUID playbackId, EchoRegion region, int durationTicks, int memorySlot,
			ClientPlaybackData data) {
		this.playbackId = playbackId;
		this.region = region;
		this.durationTicks = durationTicks;
		this.memorySlot = memorySlot;
		this.data = data;
		this.blockDeltas = new EchoBlockDeltaState(data.snapshot(), data.blockTimeline());
		this.soundPlayer = new EchoSoundPlayer(data.soundTimeline(), waves, region.centerVector());
		this.pendingSectionKeys.addAll(data.snapshot().sections().keySet());
	}

	public UUID playbackId() {
		return playbackId;
	}

	public EchoRegion region() {
		return region;
	}

	public int durationTicks() {
		return durationTicks;
	}

	public int memorySlot() {
		return memorySlot;
	}

	public int currentTick() {
		return currentTick;
	}

	public ClientPlaybackData data() {
		return data;
	}

	public EchoWaveManager waves() {
		return waves;
	}

	public EchoBlockDeltaState blockDeltas() {
		return blockDeltas;
	}

	public Int2ObjectMap<EchoSectionMesh> meshes() {
		return meshes;
	}

	public List<EchoEntityPuppet> puppets() {
		return puppets;
	}

	public void syncTo(int serverTick) {
		currentTick = serverTick;
		soundPlayer.resyncTo(serverTick);
	}

	public void clientTick(ClientLevel level) {
		currentTick++;
		soundPlayer.playForTick(level, currentTick);
		waves.prune(currentTick);
		blockDeltas.advanceTo(currentTick);
		ensurePuppets(level);
	}

	private void ensurePuppets(ClientLevel level) {
		if (puppetsInitialised) {
			return;
		}
		puppetsInitialised = true;
		for (EntityTrack track : data.entityTimeline().tracks()) {
			EchoEntityPuppet.create(data.entityTimeline(), track, level).ifPresent(puppets::add);
		}
	}

	public void buildPendingMeshes() {
		int built = 0;
		while (built < MESH_BUILDS_PER_FRAME && !pendingSectionKeys.isEmpty()) {
			int sectionKey = pendingSectionKeys.removeInt(pendingSectionKeys.size() - 1);
			EchoSectionMesh mesh = EchoSectionMesh.build(data.snapshot(), sectionKey,
					blockDeltas.trackedPositions());
			if (!mesh.isEmpty()) {
				meshes.put(sectionKey, mesh);
			}
			built++;
		}
	}

	public boolean isMeshComplete() {
		return pendingSectionKeys.isEmpty();
	}

	public Vec3 centerVector() {
		return region.centerVector();
	}

	public void discard() {
		for (EchoSectionMesh mesh : meshes.values()) {
			mesh.close();
		}
		meshes.clear();
		pendingSectionKeys.clear();
		puppets.clear();
		waves.clear();
	}
}
