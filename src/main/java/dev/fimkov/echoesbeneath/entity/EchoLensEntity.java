package dev.fimkov.echoesbeneath.entity;

import dev.fimkov.echoesbeneath.registry.EchoEntities;
import dev.fimkov.echoesbeneath.runtime.EchoLensReturn;
import net.minecraft.core.particles.ParticleOptions;
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
	private static final float BASE_SPIN_DEGREES_PER_TICK = 2.0F;
	private static final float PROGRESS_SPIN_BONUS = 6.0F;
	private static final int PARTICLE_INTERVAL_TICKS = 2;
	private static final double PARTICLE_SPREAD = 0.35D;
	private static final double RING_RADIUS = 1.05D;
	private static final int RETURN_TRAIL_PARTICLES = 2;
	private static final double ARRIVAL_TICKS_PER_BLOCK = 1.4D;
	private static final int MIN_ARRIVAL_TICKS = 12;
	private static final int MAX_ARRIVAL_TICKS = 44;
	private static final double ARRIVAL_ARC_HEIGHT = 1.8D;
	private static final float ARRIVAL_SPIN_MULTIPLIER = 4.5F;
	private static final int ARRIVAL_BURST_PARTICLES = 30;
	private static final double ARRIVAL_BURST_SPREAD = 0.8D;
	private static final int TRAIL_PARTICLES_PER_BEAT = 2;
	private static final float FULL_TURN_DEGREES = 360.0F;
	private static final double RETURN_SPEED = 0.55D;
	private static final double RETURN_PICKUP_DISTANCE = 1.1D;
	private static final int ORPHAN_GRACE_TICKS = 40;

	private static final EntityDataAccessor<ItemStack> DATA_LENS =
			SynchedEntityData.defineId(EchoLensEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Integer> DATA_MODE =
			SynchedEntityData.defineId(EchoLensEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_SPIN =
			SynchedEntityData.defineId(EchoLensEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_PROGRESS =
			SynchedEntityData.defineId(EchoLensEntity.class, EntityDataSerializers.FLOAT);

	private UUID ownerId;
	private UUID sessionId;
	private int orphanTicks;
	private Vec3 arrivalOrigin = Vec3.ZERO;
	private Vec3 arrivalTarget = Vec3.ZERO;
	private int arrivalTicksTotal;
	private int arrivalTicksRemaining;

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
		builder.define(DATA_PROGRESS, 0.0F);
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

	public float getProgress() {
		return this.entityData.get(DATA_PROGRESS);
	}

	public void setProgress(float progress) {
		this.entityData.set(DATA_PROGRESS, Math.clamp(progress, 0.0F, 1.0F));
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

	public void beginArrival(Vec3 from, Vec3 to) {
		this.arrivalOrigin = from;
		this.arrivalTarget = to;
		this.arrivalTicksTotal = (int) Math.clamp(
				Math.round(from.distanceTo(to) * ARRIVAL_TICKS_PER_BLOCK), MIN_ARRIVAL_TICKS, MAX_ARRIVAL_TICKS);
		this.arrivalTicksRemaining = this.arrivalTicksTotal;
		this.setPos(from);
	}

	public boolean isArriving() {
		return arrivalTicksRemaining > 0;
	}

	private void tickArrival(ServerLevel serverLevel) {
		arrivalTicksRemaining--;
		float travelled = 1.0F - (float) arrivalTicksRemaining / Math.max(1, arrivalTicksTotal);
		float eased = travelled * travelled * (3.0F - 2.0F * travelled);
		Vec3 flat = arrivalOrigin.add(arrivalTarget.subtract(arrivalOrigin).scale(eased));
		double arc = Math.sin(eased * Math.PI) * ARRIVAL_ARC_HEIGHT;
		this.setPos(flat.add(0.0D, arc, 0.0D));
		this.entityData.set(DATA_SPIN,
				(getSpin() + BASE_SPIN_DEGREES_PER_TICK * ARRIVAL_SPIN_MULTIPLIER) % FULL_TURN_DEGREES);
		emitTrailParticles(serverLevel);
		if (arrivalTicksRemaining == 0) {
			this.setPos(arrivalTarget);
			serverLevel.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
					arrivalTarget.x, arrivalTarget.y, arrivalTarget.z,
					ARRIVAL_BURST_PARTICLES, ARRIVAL_BURST_SPREAD, ARRIVAL_BURST_SPREAD,
					ARRIVAL_BURST_SPREAD, 0.0D);
		}
	}

	@Override
	public void tick() {
		super.tick();
		this.entityData.set(DATA_SPIN, (getSpin() + spinStep()) % FULL_TURN_DEGREES);
		if (this.level() instanceof ServerLevel serverLevel) {
			serverTick(serverLevel);
		}
	}

	private float spinStep() {
		return BASE_SPIN_DEGREES_PER_TICK + PROGRESS_SPIN_BONUS * getProgress();
	}

	private void serverTick(ServerLevel serverLevel) {
		if (isArriving()) {
			tickArrival(serverLevel);
			return;
		}
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
			emitProgressRing(serverLevel);
		}
	}

	private void emitProgressRing(ServerLevel serverLevel) {
		double leadingAngle = getProgress() * Math.PI * 2.0D;
		spawnRingParticle(serverLevel, headParticle(), leadingAngle);
		for (int trail = 0; trail < TRAIL_PARTICLES_PER_BEAT; trail++) {
			spawnRingParticle(serverLevel, trailParticle(), this.random.nextDouble() * leadingAngle);
		}
		serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY(), this.getZ(),
				1, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPREAD, 0.0D);
	}

	private void spawnRingParticle(ServerLevel serverLevel, ParticleOptions particle, double angle) {
		serverLevel.sendParticles(particle,
				this.getX() + Math.cos(angle) * RING_RADIUS,
				this.getY(),
				this.getZ() + Math.sin(angle) * RING_RADIUS,
				1, 0.0D, 0.0D, 0.0D, 0.0D);
	}

	private ParticleOptions headParticle() {
		return getMode() == EchoLensMode.PLAYBACK ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SCULK_SOUL;
	}

	private ParticleOptions trailParticle() {
		return getMode() == EchoLensMode.PLAYBACK ? ParticleTypes.WITCH : ParticleTypes.ELECTRIC_SPARK;
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
		emitTrailParticles(serverLevel);
	}

	private void emitTrailParticles(ServerLevel serverLevel) {
		serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY(), this.getZ(),
				RETURN_TRAIL_PARTICLES, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPREAD, 0.0D);
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

	private static Vec3 readVector(CompoundTag tag, String key) {
		if (!tag.contains(key)) {
			return Vec3.ZERO;
		}
		net.minecraft.nbt.ListTag values = tag.getList(key, net.minecraft.nbt.Tag.TAG_DOUBLE);
		if (values.size() != 3) {
			return Vec3.ZERO;
		}
		return new Vec3(values.getDouble(0), values.getDouble(1), values.getDouble(2));
	}

	private static void writeVector(CompoundTag tag, String key, Vec3 value) {
		net.minecraft.nbt.ListTag values = new net.minecraft.nbt.ListTag();
		values.add(net.minecraft.nbt.DoubleTag.valueOf(value.x));
		values.add(net.minecraft.nbt.DoubleTag.valueOf(value.y));
		values.add(net.minecraft.nbt.DoubleTag.valueOf(value.z));
		tag.put(key, values);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.entityData.set(DATA_MODE, tag.getInt("mode"));
		this.ownerId = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
		this.sessionId = tag.hasUUID("session") ? tag.getUUID("session") : null;
		this.orphanTicks = tag.getInt("orphan_ticks");
		this.arrivalTicksTotal = tag.getInt("arrival_total");
		this.arrivalTicksRemaining = tag.getInt("arrival_left");
		this.arrivalOrigin = readVector(tag, "arrival_origin");
		this.arrivalTarget = readVector(tag, "arrival_target");
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
		tag.putInt("arrival_total", arrivalTicksTotal);
		tag.putInt("arrival_left", arrivalTicksRemaining);
		writeVector(tag, "arrival_origin", arrivalOrigin);
		writeVector(tag, "arrival_target", arrivalTarget);
		ItemStack lens = getLens();
		if (!lens.isEmpty()) {
			tag.put("lens", lens.save(this.registryAccess()));
		}
	}
}
