package dev.fimkov.echoesbeneath.entity;

import dev.fimkov.echoesbeneath.registry.EchoEntities;
import dev.fimkov.echoesbeneath.runtime.EchoLensReturn;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class EchoLensEntity extends Entity {
	public static final double HOVER_HEIGHT = 1.375D;
	private static final float SPIN_DEGREES_PER_TICK = 2.5F;
	private static final int PARTICLE_INTERVAL_TICKS = 6;
	private static final double PARTICLE_SPREAD = 0.35D;
	private static final int PARTICLE_COUNT = 2;
	private static final double RETURN_SPEED = 0.55D;
	private static final double RETURN_PICKUP_DISTANCE = 1.1D;
	private static final int ORPHAN_GRACE_TICKS = 40;

	private static final EntityDataAccessor<ItemStack> DATA_LENS =
			SynchedEntityData.defineId(EchoLensEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Integer> DATA_MODE =
			SynchedEntityData.defineId(EchoLensEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_SPIN =
			SynchedEntityData.defineId(EchoLensEntity.class, EntityDataSerializers.FLOAT);

	private UUID ownerId;
	private UUID sessionId;
	private int orphanTicks;

	public EchoLensEntity(EntityType<? extends EchoLensEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	public EchoLensEntity(Level level, Vec3 position, ItemStack lens, UUID ownerId, UUID sessionId, EchoLensMode mode) {
		this(EchoEntities.ECHO_LENS, level);
		this.setPos(position);
		this.setLens(lens);
		this.ownerId = ownerId;
		this.sessionId = sessionId;
		this.setMode(mode);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_LENS, ItemStack.EMPTY);
		builder.define(DATA_MODE, EchoLensMode.RECORDING.ordinal());
		builder.define(DATA_SPIN, 0.0F);
	}

	public ItemStack getLens() {
		return this.entityData.get(DATA_LENS);
	}

	public void setLens(ItemStack stack) {
		this.entityData.set(DATA_LENS, stack.copy());
	}

	public ItemStack takeLens() {
		ItemStack held = getLens();
		this.entityData.set(DATA_LENS, ItemStack.EMPTY);
		return held;
	}

	public EchoLensMode getMode() {
		return EchoLensMode.fromIndex(this.entityData.get(DATA_MODE));
	}

	public void setMode(EchoLensMode mode) {
		this.entityData.set(DATA_MODE, mode.ordinal());
	}

	public float getSpin() {
		return this.entityData.get(DATA_SPIN);
	}

	public Optional<UUID> getOwnerId() {
		return Optional.ofNullable(ownerId);
	}

	public Optional<UUID> getSessionId() {
		return Optional.ofNullable(sessionId);
	}

	public void setSessionId(UUID sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public void tick() {
		super.tick();
		this.entityData.set(DATA_SPIN, (getSpin() + SPIN_DEGREES_PER_TICK) % 360.0F);
		if (this.level() instanceof ServerLevel serverLevel) {
			serverTick(serverLevel);
		}
	}

	private void serverTick(ServerLevel serverLevel) {
		if (getMode() == EchoLensMode.RETURNING) {
			tickReturn(serverLevel);
			return;
		}
		if (getMode() == EchoLensMode.ABANDONED) {
			EchoLensReturn.dropAtSelf(this);
			return;
		}
		if (!EchoLensReturn.hasLiveSession(serverLevel, this)) {
			orphanTicks++;
			if (orphanTicks > ORPHAN_GRACE_TICKS) {
				EchoLensReturn.dropAtSelf(this);
			}
			return;
		}
		orphanTicks = 0;
		if (this.tickCount % PARTICLE_INTERVAL_TICKS == 0) {
			emitAmbientParticles(serverLevel);
		}
	}

	private void tickReturn(ServerLevel serverLevel) {
		Player owner = ownerId == null ? null : serverLevel.getPlayerByUUID(ownerId);
		if (owner == null) {
			EchoLensReturn.dropAtSelf(this);
			return;
		}
		Vec3 target = owner.position().add(0.0D, owner.getBbHeight() * 0.5D, 0.0D);
		Vec3 delta = target.subtract(this.position());
		if (delta.length() <= RETURN_PICKUP_DISTANCE) {
			EchoLensReturn.deliverTo(this, owner);
			return;
		}
		this.setPos(this.position().add(delta.normalize().scale(RETURN_SPEED)));
		emitAmbientParticles(serverLevel);
	}

	private void emitAmbientParticles(ServerLevel serverLevel) {
		serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY(), this.getZ(),
				PARTICLE_COUNT, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPREAD, 0.0D);
		serverLevel.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY(), this.getZ(),
				1, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPREAD, 0.0D);
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (this.level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (ownerId != null && !ownerId.equals(player.getUUID())) {
			return InteractionResult.PASS;
		}
		return EchoLensReturn.requestEarlyStop(this, player) ? InteractionResult.CONSUME : InteractionResult.PASS;
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return true;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.entityData.set(DATA_MODE, tag.getInt("mode"));
		this.ownerId = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
		this.sessionId = tag.hasUUID("session") ? tag.getUUID("session") : null;
		this.orphanTicks = tag.getInt("orphan_ticks");
		if (tag.contains("lens")) {
			this.setLens(ItemStack.parse(this.registryAccess(), tag.getCompound("lens")).orElse(ItemStack.EMPTY));
		}
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("mode", this.entityData.get(DATA_MODE));
		if (ownerId != null) {
			tag.putUUID("owner", ownerId);
		}
		if (sessionId != null) {
			tag.putUUID("session", sessionId);
		}
		tag.putInt("orphan_ticks", orphanTicks);
		ItemStack lens = getLens();
		if (!lens.isEmpty()) {
			tag.put("lens", lens.save(this.registryAccess()));
		}
	}
}
