package dev.fimkov.echoesbeneath.entity;

import dev.fimkov.echoesbeneath.chart.EchoChartData;
import dev.fimkov.echoesbeneath.registry.EchoComponents;
import dev.fimkov.echoesbeneath.registry.EchoEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EchoChartEntity extends net.minecraft.world.entity.Entity {
	public static final float SPIN_DEGREES_PER_TICK = 0.35F;
	public static final int TURN_STEPS = 16;
	public static final int FREE_SPIN = -1;
	private static final float DEGREES_PER_STEP = 360.0F / TURN_STEPS;
	private static final float BOB_PERIOD_TICKS = 110.0F;
	private static final double BOB_HEIGHT = 0.07D;
	private static final String TAG_CHART = "chart";
	private static final String TAG_TURN = "turn";

	private static final EntityDataAccessor<ItemStack> DATA_CHART =
			SynchedEntityData.defineId(EchoChartEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Integer> DATA_TURN =
			SynchedEntityData.defineId(EchoChartEntity.class, EntityDataSerializers.INT);

	private float spin;

	public EchoChartEntity(EntityType<? extends EchoChartEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	public EchoChartEntity(Level level, ItemStack chart) {
		this(EchoEntities.ECHO_CHART, level);
		setChart(chart.copyWithCount(1));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_CHART, ItemStack.EMPTY);
		builder.define(DATA_TURN, FREE_SPIN);
	}

	public ItemStack chart() {
		return entityData.get(DATA_CHART);
	}

	public void setChart(ItemStack stack) {
		entityData.set(DATA_CHART, stack);
	}

	public EchoChartData chartData() {
		return chart().get(EchoComponents.CHART_DATA);
	}

	public float spin(float partialTick) {
		int turn = entityData.get(DATA_TURN);
		if (turn != FREE_SPIN) {
			return turn * DEGREES_PER_STEP;
		}
		return spin + SPIN_DEGREES_PER_TICK * partialTick;
	}

	private void turnOnce() {
		int turn = entityData.get(DATA_TURN);
		entityData.set(DATA_TURN, turn + 1 >= TURN_STEPS ? FREE_SPIN : turn + 1);
	}

	public double bob(float partialTick) {
		double phase = (tickCount + partialTick) / BOB_PERIOD_TICKS * Math.PI * 2.0D;
		return Math.sin(phase) * BOB_HEIGHT;
	}

	@Override
	public void tick() {
		super.tick();
		if (entityData.get(DATA_TURN) != FREE_SPIN) {
			return;
		}
		spin += SPIN_DEGREES_PER_TICK;
		if (spin >= 360.0F) {
			spin -= 360.0F;
		}
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			giveBack(player);
			return InteractionResult.CONSUME;
		}
		turnOnce();
		return InteractionResult.CONSUME;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (level().isClientSide() || isRemoved()) {
			return false;
		}
		if (source.getEntity() instanceof Player player) {
			giveBack(player);
			return true;
		}
		return false;
	}

	private void giveBack(Player player) {
		ItemStack stack = chart();
		if (!stack.isEmpty() && !player.getAbilities().instabuild
				&& !player.getInventory().add(stack.copy())) {
			player.drop(stack.copy(), false);
		}
		discard();
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
	protected void readAdditionalSaveData(CompoundTag tag) {
		if (tag.contains(TAG_CHART)) {
			setChart(ItemStack.parse(registryAccess(), tag.getCompound(TAG_CHART)).orElse(ItemStack.EMPTY));
		}
		entityData.set(DATA_TURN, tag.contains(TAG_TURN)
				? Math.clamp(tag.getInt(TAG_TURN), FREE_SPIN, TURN_STEPS - 1)
				: FREE_SPIN);
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		ItemStack stack = chart();
		if (!stack.isEmpty()) {
			tag.put(TAG_CHART, stack.save(registryAccess()));
		}
		tag.putInt(TAG_TURN, entityData.get(DATA_TURN));
	}
}
