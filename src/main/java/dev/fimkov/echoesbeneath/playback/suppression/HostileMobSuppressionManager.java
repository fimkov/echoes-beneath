package dev.fimkov.echoesbeneath.playback.suppression;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.playback.EchoRegion;
import dev.fimkov.echoesbeneath.registry.EchoTags;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class HostileMobSuppressionManager {
	private final MinecraftServer server;

	public HostileMobSuppressionManager(MinecraftServer server) {
		this.server = server;
	}

	private EchoSuppressionStore store() {
		return EchoSuppressionStore.of(server);
	}

	public static boolean isSuppressible(Entity entity) {
		return entity instanceof Mob && entity.getType().is(EchoTags.TEMPORARILY_SUPPRESSED);
	}

	public Map<UUID, SuppressedMobState> suppressRegion(ServerLevel level, EchoRegion region) {
		Map<UUID, SuppressedMobState> captured = new HashMap<>();
		List<Entity> candidates = level.getEntities((Entity) null, region.bounds(),
				entity -> isSuppressible(entity) && region.contains(level.dimension(), entity.position()));

		for (Entity entity : candidates) {
			suppress(entity).ifPresent(state -> captured.put(state.entityId(), state));
		}

		return captured;
	}

	public Optional<SuppressedMobState> suppress(Entity entity) {
		if (!(entity instanceof EchoSuppressedEntity suppressible) || suppressible.echoesBeneath$isSuppressed()) {
			return Optional.empty();
		}

		SuppressedMobState state = SuppressedMobState.capture(entity);
		store().remember(state);
		applySuppression(entity);
		suppressible.echoesBeneath$setSuppressed(true);

		return Optional.of(state);
	}

	private void applySuppression(Entity entity) {
		entity.setInvisible(true);
		entity.setSilent(true);
		entity.setInvulnerable(true);
		entity.setDeltaMovement(Vec3.ZERO);

		if (entity instanceof Mob mob) {
			mob.setNoAi(true);
			mob.setTarget(null);
			mob.setPersistenceRequired();
		}
	}

	public void restoreAll(Map<UUID, SuppressedMobState> states) {
		for (Map.Entry<UUID, SuppressedMobState> entry : states.entrySet()) {
			restore(entry.getKey(), entry.getValue());
		}
	}

	public boolean restore(UUID entityId, SuppressedMobState state) {
		ServerLevel level = server.getLevel(state.dimension());

		if (level == null) {
			return false;
		}

		Entity entity = level.getEntity(entityId);

		if (entity == null) {
			return false;
		}

		return restoreEntity(entity, state);
	}

	public boolean restoreEntity(Entity entity, SuppressedMobState state) {
		try {
			state.restoreTo(entity);

			if (entity instanceof EchoSuppressedEntity suppressible) {
				suppressible.echoesBeneath$setSuppressed(false);
			}
			store().forget(entity.getUUID());

			return true;
		} catch (RuntimeException failure) {
			EchoesBeneath.LOGGER.error("Unable to restore suppressed mob {}", entity.getUUID(), failure);

			return false;
		}
	}

	public boolean restoreOnLoad(Entity entity) {
		Optional<SuppressedMobState> remembered = Optional.ofNullable(store().snapshot().get(entity.getUUID()));

		if (remembered.isEmpty()) {
			return false;
		}

		return restoreEntity(entity, remembered.get());
	}

	public int recoverEverything() {
		Map<UUID, SuppressedMobState> pending = store().snapshot();
		int restored = 0;

		for (Map.Entry<UUID, SuppressedMobState> entry : pending.entrySet()) {
			if (restore(entry.getKey(), entry.getValue())) {
				restored++;
			}
		}

		return restored;
	}

	public int pendingCount() {
		return store().pendingCount();
	}
}
