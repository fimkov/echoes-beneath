package dev.fimkov.echoesbeneath.recording;

import dev.fimkov.echoesbeneath.entity.EchoLensEntity;
import dev.fimkov.echoesbeneath.memory.timeline.EchoEntityAction;
import dev.fimkov.echoesbeneath.memory.timeline.EntityTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.EntityTrack;
import dev.fimkov.echoesbeneath.registry.EchoTags;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class EntityTrackRecorder {
	private static final double POSITION_EPSILON = 0.02D;
	private static final float ROTATION_EPSILON = 1.0F;
	private static final double TELEPORT_DISTANCE = 8.0D;
	private static final int FORCED_KEYFRAME_INTERVAL = 20;
	private static final EquipmentSlot[] TRACKED_SLOTS = {
			EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private final EntityTimeline timeline;
	private final BlockPos center;
	private final Map<UUID, TrackedEntity> tracked = new HashMap<>();

	public EntityTrackRecorder(EntityTimeline timeline, BlockPos center) {
		this.timeline = timeline;
		this.center = center;
	}

	public static boolean isRecordable(Entity entity) {
		if (entity instanceof EchoLensEntity) {
			return false;
		}
		return !entity.getType().is(EchoTags.RECORDING_BLACKLIST);
	}

	public void sample(Entity entity, int tick) {
		TrackedEntity state = tracked.get(entity.getUUID());
		if (state == null) {
			if (!timeline.hasCapacity()) {
				return;
			}
			ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
			state = new TrackedEntity(timeline.createTrack(typeId));
			tracked.put(entity.getUUID(), state);
			writeKeyframe(state, entity, tick);
			captureEquipment(state, entity, tick, true);
			return;
		}
		double distance = distanceMovedSince(state, entity);
		boolean teleported = distance > TELEPORT_DISTANCE;
		if (teleported) {
			state.track.addAction(tick, EchoEntityAction.TELEPORT);
		}
		if (teleported || needsKeyframe(state, entity, tick)) {
			writeKeyframe(state, entity, tick);
		}
		captureActions(state, entity, tick);
		captureEquipment(state, entity, tick, false);
	}

	private double distanceMovedSince(TrackedEntity state, Entity entity) {
		double dx = entity.getX() - (center.getX() + state.lastX);
		double dy = entity.getY() - (center.getY() + state.lastY);
		double dz = entity.getZ() - (center.getZ() + state.lastZ);
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private boolean needsKeyframe(TrackedEntity state, Entity entity, int tick) {
		if (tick - state.lastKeyframeTick >= FORCED_KEYFRAME_INTERVAL) {
			return true;
		}
		if (distanceMovedSince(state, entity) > POSITION_EPSILON) {
			return true;
		}
		if (Math.abs(entity.getYRot() - state.lastYaw) > ROTATION_EPSILON) {
			return true;
		}
		if (Math.abs(entity.getXRot() - state.lastPitch) > ROTATION_EPSILON) {
			return true;
		}
		return entity.onGround() != state.lastOnGround || entity.getPose().ordinal() != state.lastPose;
	}

	private void writeKeyframe(TrackedEntity state, Entity entity, int tick) {
		double relativeX = entity.getX() - center.getX();
		double relativeY = entity.getY() - center.getY();
		double relativeZ = entity.getZ() - center.getZ();
		float headYaw = entity instanceof LivingEntity living ? living.getYHeadRot() : entity.getYRot();
		boolean baby = entity instanceof LivingEntity living && living.isBaby();
		state.track.addKeyframe(tick, relativeX, relativeY, relativeZ, entity.getYRot(), entity.getXRot(),
				headYaw, entity.onGround(), entity.getPose().ordinal(), baby);
		state.lastX = relativeX;
		state.lastY = relativeY;
		state.lastZ = relativeZ;
		state.lastYaw = entity.getYRot();
		state.lastPitch = entity.getXRot();
		state.lastOnGround = entity.onGround();
		state.lastPose = entity.getPose().ordinal();
		state.lastKeyframeTick = tick;
	}

	private void captureActions(TrackedEntity state, Entity entity, int tick) {
		if (!(entity instanceof LivingEntity living)) {
			return;
		}
		if (living.swinging && !state.wasSwinging) {
			state.track.addAction(tick, EchoEntityAction.SWING);
		}
		state.wasSwinging = living.swinging;

		boolean hurt = living.hurtTime > 0;
		if (hurt && !state.wasHurt) {
			state.track.addAction(tick, EchoEntityAction.HURT);
		}
		state.wasHurt = hurt;

		boolean using = living.isUsingItem();
		if (using && !state.wasUsingItem) {
			state.track.addAction(tick, EchoEntityAction.USE_ITEM);
		}
		state.wasUsingItem = using;

		boolean dead = living.isDeadOrDying();
		if (dead && !state.wasDead) {
			state.track.addAction(tick, EchoEntityAction.DEATH);
		}
		state.wasDead = dead;

		boolean airborne = !living.onGround();
		if (airborne && !state.wasAirborne && living.getDeltaMovement().y > 0.0D) {
			state.track.addAction(tick, EchoEntityAction.JUMP);
		}
		state.wasAirborne = airborne;
	}

	private void captureEquipment(TrackedEntity state, Entity entity, int tick, boolean force) {
		if (!(entity instanceof LivingEntity living)) {
			return;
		}
		for (int slotIndex = 0; slotIndex < TRACKED_SLOTS.length; slotIndex++) {
			ItemStack stack = living.getItemBySlot(TRACKED_SLOTS[slotIndex]);
			ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
			int itemIndex = timeline.internItem(itemId);
			if (force || state.lastEquipment[slotIndex] != itemIndex) {
				state.track.addEquipment(tick, slotIndex, itemIndex);
				state.lastEquipment[slotIndex] = itemIndex;
			}
		}
	}

	public int trackedCount() {
		return tracked.size();
	}

	private static final class TrackedEntity {
		private final EntityTrack track;
		private final int[] lastEquipment = new int[TRACKED_SLOTS.length];
		private double lastX;
		private double lastY;
		private double lastZ;
		private float lastYaw;
		private float lastPitch;
		private boolean lastOnGround;
		private int lastPose;
		private int lastKeyframeTick = Integer.MIN_VALUE;
		private boolean wasSwinging;
		private boolean wasHurt;
		private boolean wasUsingItem;
		private boolean wasDead;
		private boolean wasAirborne;

		private TrackedEntity(EntityTrack track) {
			this.track = track;
			Arrays.fill(lastEquipment, EntityTimeline.NO_ITEM_INDEX);
		}
	}
}
