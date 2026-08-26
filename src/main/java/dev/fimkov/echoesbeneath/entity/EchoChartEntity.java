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
	private static final float BOB_PERIOD_TICKS = 110.0F;
	private static final double BOB_HEIGHT = 0.07D;
	private static final String TAG_CHART = "chart";

	private static final EntityDataAccessor<ItemStack> DATA_CHART =
			SynchedEntityData.defineId(EchoChartEntity.class, EntityDataSerializers.ITEM_STACK);

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
		return spin + SPIN_DEGREES_PER_TICK * partialTick;
	}

	public double bob(float partialTick) {
		double phase = (tickCount + partialTick) / BOB_PERIOD_TICKS * Math.PI * 2.0D;
		return Math.sin(phase) * BOB_HEIGHT;
	}

	@Override
	public void tick() {
		super.tick();
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
		giveBack(player);
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
		if (!stack.isEmpty() && !player.getInventory().add(stack.copy())) {
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
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		ItemStack stack = chart();
		if (!stack.isEmpty()) {
			tag.put(TAG_CHART, stack.save(registryAccess()));
		}
	}
}
