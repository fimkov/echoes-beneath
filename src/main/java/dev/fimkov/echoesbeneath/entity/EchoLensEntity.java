package dev.fimkov.echoesbeneath.entity;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.UUIDUtil;
import dev.fimkov.echoesbeneath.registry.EchoEntities;
import dev.fimkov.echoesbeneath.runtime.EchoLensReturn;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

public class EchoLensEntity extends Entity {
	public static final double HOVER_HEIGHT = 1.375D;
	private static final float BASE_SPIN_DEGREES_PER_TICK = 2.0F;
	private static final int GLOW_COLOUR = 0x29DFEB;
	private static final float PROGRESS_SPIN_BONUS = 6.0F;
	private static final int PARTICLE_INTERVAL_TICKS = 2;
	private static final double PARTICLE_SPREAD = 0.35D;
	private static final double AURA_SPREAD = 0.28D;
	private static final double AURA_DRIFT = 0.012D;
	private static final int RETURN_TRAIL_PARTICLES = 2;
	private static final double ARRIVAL_TICKS_PER_BLOCK = 1.4D;
	private static final int MIN_ARRIVAL_TICKS = 12;
	private static final int MAX_ARRIVAL_TICKS = 44;
	private static final double ARRIVAL_ARC_HEIGHT = 1.8D;
	private static final float ARRIVAL_SPIN_MULTIPLIER = 4.5F;
	private static final int ARRIVAL_BURST_PARTICLES = 30;
	private static final double ARRIVAL_BURST_SPREAD = 0.8D;
	private static final int MIN_AURA_PARTICLES = 1;
	private static final int MAX_AURA_PARTICLES = 5;
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
			emitProgressAura(serverLevel);
		}
	}

	private void emitProgressAura(ServerLevel serverLevel) {
		float progress = getProgress();
		int count = MIN_AURA_PARTICLES + Math.round(progress * (MAX_AURA_PARTICLES - MIN_AURA_PARTICLES));

		for (int index = 0; index < count; index++) {
			serverLevel.sendParticles(auraParticle(),
					this.getX(), this.getY(), this.getZ(),
					1, AURA_SPREAD, AURA_SPREAD, AURA_SPREAD, AURA_DRIFT * progress);
		}
	}

	private ParticleOptions auraParticle() {
		return getMode() == EchoLensMode.PLAYBACK ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SCULK_SOUL;
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
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitPos) {
		if (this.level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (ownerId != null && !ownerId.equals(player.getUUID())) {
			return InteractionResult.PASS;
		}

		return EchoLensReturn.requestEarlyStop(this, player) ? InteractionResult.CONSUME : InteractionResult.PASS;
	}

	@Override
	public boolean isCurrentlyGlowing() {
		return true;
	}

	@Override
	public int getTeamColor() {
		return GLOW_COLOUR;
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
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isInvulnerable() {
		return true;
	}

	private static Vec3 readVector(ValueInput input, String key) {
		return input.read(key, Vec3.CODEC).orElse(Vec3.ZERO);
	}

	private static void writeVector(ValueOutput output, String key, Vec3 value) {
		output.store(key, Vec3.CODEC, value);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.entityData.set(DATA_MODE, input.getIntOr("mode", 0));
		this.ownerId = input.getIntArray("owner").map(UUIDUtil::uuidFromIntArray).orElse(null);
		this.sessionId = input.getIntArray("session").map(UUIDUtil::uuidFromIntArray).orElse(null);
		this.orphanTicks = input.getIntOr("orphan_ticks", 0);
		this.arrivalTicksTotal = input.getIntOr("arrival_total", 0);
		this.arrivalTicksRemaining = input.getIntOr("arrival_left", 0);
		this.arrivalOrigin = readVector(input, "arrival_origin");
		this.arrivalTarget = readVector(input, "arrival_target");

		this.setLens(input.read("lens", ItemStack.CODEC).orElse(ItemStack.EMPTY));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("mode", this.entityData.get(DATA_MODE));

		if (ownerId != null) {
			output.putIntArray("owner", UUIDUtil.uuidToIntArray(ownerId));
		}

		if (sessionId != null) {
			output.putIntArray("session", UUIDUtil.uuidToIntArray(sessionId));
		}

		output.putInt("orphan_ticks", orphanTicks);
		output.putInt("arrival_total", arrivalTicksTotal);
		output.putInt("arrival_left", arrivalTicksRemaining);
		writeVector(output, "arrival_origin", arrivalOrigin);
		writeVector(output, "arrival_target", arrivalTarget);

		ItemStack lens = getLens();

		if (!lens.isEmpty()) {
			output.store("lens", ItemStack.CODEC, lens);
		}
	}
}
