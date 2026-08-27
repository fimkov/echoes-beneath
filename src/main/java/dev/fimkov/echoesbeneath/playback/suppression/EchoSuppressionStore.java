package dev.fimkov.echoesbeneath.playback.suppression;

import com.mojang.serialization.Codec;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EchoSuppressionStore extends SavedData {
	public static final String STORAGE_KEY = "echoes_beneath_suppressed_mobs";

	public static final Codec<EchoSuppressionStore> CODEC = CompoundTag.CODEC.listOf().xmap(
			EchoSuppressionStore::fromTags, EchoSuppressionStore::toTags);

	public static final SavedDataType<EchoSuppressionStore> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath(EchoesBeneath.MOD_ID, "suppressed_mobs"),
			EchoSuppressionStore::new,
			CODEC,
			DataFixTypes.LEVEL);

	private final Map<UUID, SuppressedMobState> pending = new HashMap<>();

	public static EchoSuppressionStore of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	private static EchoSuppressionStore fromTags(List<CompoundTag> tags) {
		EchoSuppressionStore store = new EchoSuppressionStore();

		for (CompoundTag tag : tags) {
			try {
				SuppressedMobState state = SuppressedMobState.fromNbt(tag);
				store.pending.put(state.entityId(), state);
			} catch (RuntimeException failure) {
				EchoesBeneath.LOGGER.warn("Dropping an unreadable suppressed mob entry", failure);
			}
		}

		return store;
	}

	private static List<CompoundTag> toTags(EchoSuppressionStore store) {
		List<CompoundTag> tags = new ArrayList<>(store.pending.size());

		for (SuppressedMobState state : store.pending.values()) {
			tags.add(state.toNbt());
		}

		return tags;
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
