package dev.fimkov.echoesbeneath.playback.suppression;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EchoSuppressionStore extends SavedData {
	public static final String STORAGE_KEY = "echoes_beneath_suppressed_mobs";
	private static final int NBT_COMPOUND_TYPE = Tag.TAG_COMPOUND;

	private final Map<UUID, SuppressedMobState> pending = new HashMap<>();

	public static SavedData.Factory<EchoSuppressionStore> factory() {
		return new SavedData.Factory<>(EchoSuppressionStore::new, EchoSuppressionStore::load, null);
	}

	public static EchoSuppressionStore of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(factory(), STORAGE_KEY);
	}

	private static EchoSuppressionStore load(CompoundTag tag, HolderLookup.Provider lookup) {
		EchoSuppressionStore store = new EchoSuppressionStore();
		for (Tag element : tag.getList("states", NBT_COMPOUND_TYPE)) {
			try {
				SuppressedMobState state = SuppressedMobState.fromNbt((CompoundTag) element);
				store.pending.put(state.entityId(), state);
			} catch (RuntimeException failure) {
				EchoesBeneath.LOGGER.warn("Dropping an unreadable suppressed mob entry", failure);
			}
		}
		return store;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
		ListTag states = new ListTag();
		for (SuppressedMobState state : pending.values()) {
			states.add(state.toNbt());
		}
		tag.put("states", states);
		return tag;
	}

	public void remember(SuppressedMobState state) {
		pending.put(state.entityId(), state);
		setDirty();
	}

	public Optional<SuppressedMobState> forget(UUID entityId) {
		SuppressedMobState removed = pending.remove(entityId);
		if (removed != null) {
			setDirty();
		}
		return Optional.ofNullable(removed);
	}

	public boolean isRemembered(UUID entityId) {
		return pending.containsKey(entityId);
	}

	public Map<UUID, SuppressedMobState> snapshot() {
		return Map.copyOf(pending);
	}

	public int pendingCount() {
		return pending.size();
	}

	public void clear() {
		if (!pending.isEmpty()) {
			pending.clear();
			setDirty();
		}
	}
}
