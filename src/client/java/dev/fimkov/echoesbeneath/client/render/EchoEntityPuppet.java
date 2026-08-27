package dev.fimkov.echoesbeneath.client.render;

import net.minecraft.world.entity.EntityTypes;
import dev.fimkov.echoesbeneath.memory.timeline.EntityTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.EntityTrack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public final class EchoEntityPuppet {
	private static final String REMEMBERED_PLAYER_NAME = "Echo";
	private static final EquipmentSlot[] TRACKED_SLOTS = {
			EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};
	private static final Pose[] POSES = Pose.values();
	private static final float WALK_ANIMATION_SCALE = 4.0F;

	private final EntityTrack track;
	private final Entity entity;
	private int keyframeCursor;
	private double lastX;
	private double lastZ;
	private boolean seeded;

	private EchoEntityPuppet(EntityTrack track, Entity entity) {
		this.track = track;
		this.entity = entity;
	}

	private static Entity playerStandIn(Level level) {
		if (!(level instanceof ClientLevel clientLevel)) {
			return null;
		}

		return new RemotePlayer(clientLevel, new GameProfile(UUID.randomUUID(), REMEMBERED_PLAYER_NAME));
	}

	public static Optional<EchoEntityPuppet> create(EntityTimeline timeline, EntityTrack track, Level level) {
		if (track.keyframeCount() == 0) {
			return Optional.empty();
		}

		Identifier typeId = timeline.entityTypeOf(track);
		Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(typeId);

		if (type.isEmpty()) {
			return Optional.empty();
		}

		Entity created = type.get() == EntityTypes.PLAYER
				? playerStandIn(level)
				: type.get().create(level);

		if (created == null) {
			return Optional.empty();
		}

		created.setNoGravity(true);
		created.setSilent(true);
		created.noPhysics = true;

		return Optional.of(new EchoEntityPuppet(track, created));
	}

	public Entity entity() {
		return entity;
	}

	public boolean isVisibleAt(int tick) {
		return tick >= track.keyframeTick(0) && tick <= track.keyframeTick(track.keyframeCount() - 1) + 20;
	}

	public void updateTo(EntityTimeline timeline, double centerX, double centerY, double centerZ,
			int tick, float partialTick) {
		int index = findKeyframeIndex(tick);
		int next = Math.min(index + 1, track.keyframeCount() - 1);
		float blend = interpolationFactor(index, next, tick, partialTick);

		double relativeX = lerp(track.keyframeX(index), track.keyframeX(next), blend);
		double relativeY = lerp(track.keyframeY(index), track.keyframeY(next), blend);
		double relativeZ = lerp(track.keyframeZ(index), track.keyframeZ(next), blend);
		float yaw = lerpAngle(track.keyframeYaw(index), track.keyframeYaw(next), blend);
		float pitch = lerpAngle(track.keyframePitch(index), track.keyframePitch(next), blend);
		float headYaw = lerpAngle(track.keyframeHeadYaw(index), track.keyframeHeadYaw(next), blend);

		double worldX = centerX + relativeX;
		double worldY = centerY + relativeY;
		double worldZ = centerZ + relativeZ;

		entity.setPos(worldX, worldY, worldZ);
		entity.setOldPosAndRot();
		entity.setYRot(yaw);
		entity.setXRot(pitch);
		entity.yRotO = yaw;
		entity.xRotO = pitch;
		entity.setPose(poseOf(track.keyframePose(index)));
		entity.setOnGround(track.keyframeOnGround(index));
		applyLivingState(timeline, headYaw, yaw, tick, worldX, worldZ);
	}

	private void applyLivingState(EntityTimeline timeline, float headYaw, float bodyYaw, int tick,
			double worldX, double worldZ) {
		if (!(entity instanceof LivingEntity living)) {
			return;
		}

		living.setYHeadRot(headYaw);
		living.setYBodyRot(bodyYaw);
		living.yHeadRotO = headYaw;
		living.yBodyRotO = bodyYaw;
		double travelled = seeded ? Math.hypot(worldX - lastX, worldZ - lastZ) : 0.0D;
		living.walkAnimation.update((float) travelled * WALK_ANIMATION_SCALE, 1.0F);
		lastX = worldX;
		lastZ = worldZ;
		seeded = true;
		applyEquipment(timeline, living, tick);
	}

	private void applyEquipment(EntityTimeline timeline, LivingEntity living, int tick) {
		for (int index = 0; index < track.equipmentCount(); index++) {
			if (track.equipmentTick(index) > tick) {
				break;
			}
			int slotIndex = track.equipmentSlot(index);

			if (slotIndex < 0 || slotIndex >= TRACKED_SLOTS.length) {
				continue;
			}
			Identifier itemId = timeline.itemAt(track.equipmentItem(index));
			Item item = BuiltInRegistries.ITEM.get(itemId);
			living.setItemSlot(TRACKED_SLOTS[slotIndex], new ItemStack(item));
		}
	}

	private int findKeyframeIndex(int tick) {
		int count = track.keyframeCount();

		if (keyframeCursor >= count) {
			keyframeCursor = count - 1;
		}

		while (keyframeCursor > 0 && track.keyframeTick(keyframeCursor) > tick) {
			keyframeCursor--;
		}

		while (keyframeCursor + 1 < count && track.keyframeTick(keyframeCursor + 1) <= tick) {
			keyframeCursor++;
		}

		return keyframeCursor;
	}

	private float interpolationFactor(int index, int next, int tick, float partialTick) {
		if (index == next) {
			return 0.0F;
		}

		int span = track.keyframeTick(next) - track.keyframeTick(index);

		if (span <= 0) {
			return 0.0F;
		}

		float elapsed = tick - track.keyframeTick(index) + partialTick;

		return Math.clamp(elapsed / span, 0.0F, 1.0F);
	}

	private static Pose poseOf(int ordinal) {
		if (ordinal < 0 || ordinal >= POSES.length) {
			return Pose.STANDING;
		}

		return POSES[ordinal];
	}

	private static double lerp(double from, double to, float blend) {
		return from + (to - from) * blend;
	}

	private static float lerpAngle(float from, float to, float blend) {
		float delta = to - from;

		while (delta < -180.0F) {
			delta += 360.0F;
		}

		while (delta >= 180.0F) {
			delta -= 360.0F;
		}

		return from + delta * blend;
	}
}
