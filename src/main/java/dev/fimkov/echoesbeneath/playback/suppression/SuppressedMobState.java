package dev.fimkov.echoesbeneath.playback.suppression;

import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SuppressedMobState(
		UUID entityId,
		ResourceKey<Level> dimension,
		Vec3 position,
		Vec3 velocity,
		float yRot,
		float xRot,
		float health,
		int remainingFireTicks,
		int ticksFrozen,
		boolean invisible,
		boolean silent,
		boolean invulnerable,
		boolean noAi,
		UUID targetId,
		List<CompoundTag> effects) {

	private static final int NBT_COMPOUND_TYPE = Tag.TAG_COMPOUND;

	public static SuppressedMobState capture(Entity entity) {
		float health = entity instanceof LivingEntity living ? living.getHealth() : 0.0F;
		boolean noAi = entity instanceof Mob mob && mob.isNoAi();
		UUID targetId = null;

		if (entity instanceof Mob mob && mob.getTarget() != null) {
			targetId = mob.getTarget().getUUID();
		}

		List<CompoundTag> effects = new ArrayList<>();

		if (entity instanceof LivingEntity living) {
			for (MobEffectInstance instance : living.getActiveEffects()) {
				Tag saved = MobEffectInstance.CODEC.encodeStart(NbtOps.INSTANCE, instance).result().orElse(null);

				if (saved instanceof CompoundTag compound) {
					effects.add(compound);
				}
			}
		}

		return new SuppressedMobState(
				entity.getUUID(),
				entity.level().dimension(),
				entity.position(),
				entity.getDeltaMovement(),
				entity.getYRot(),
				entity.getXRot(),
				health,
				entity.getRemainingFireTicks(),
				entity.getTicksFrozen(),
				entity.isInvisible(),
				entity.isSilent(),
				entity.isInvulnerable(),
				noAi,
				targetId,
				List.copyOf(effects));
	}

	public void restoreTo(Entity entity) {
		entity.setPos(position);
		entity.setDeltaMovement(velocity);
		entity.setYRot(yRot);
		entity.setXRot(xRot);
		entity.setRemainingFireTicks(remainingFireTicks);
		entity.setTicksFrozen(ticksFrozen);
		entity.setInvisible(invisible);
		entity.setSilent(silent);
		entity.setInvulnerable(invulnerable);

		if (entity instanceof Mob mob) {
			mob.setNoAi(noAi);
			mob.setTarget(null);
		}

		if (entity instanceof LivingEntity living) {
			living.setHealth(health);
			living.removeAllEffects();

			for (CompoundTag saved : effects) {
				MobEffectInstance instance = MobEffectInstance.CODEC.parse(NbtOps.INSTANCE, saved).result().orElse(null);

				if (instance != null) {
					living.addEffect(instance);
				}
			}
		}
	}

	public CompoundTag toNbt() {
		CompoundTag tag = new CompoundTag();
		tag.putIntArray("entity", UUIDUtil.uuidToIntArray(entityId));
		tag.putString("dimension", dimension.identifier().toString());
		tag.putDouble("x", position.x);
		tag.putDouble("y", position.y);
		tag.putDouble("z", position.z);
		tag.putDouble("vx", velocity.x);
		tag.putDouble("vy", velocity.y);
		tag.putDouble("vz", velocity.z);
		tag.putFloat("yaw", yRot);
		tag.putFloat("pitch", xRot);
		tag.putFloat("health", health);
		tag.putInt("fire", remainingFireTicks);
		tag.putInt("freeze", ticksFrozen);
		tag.putBoolean("invisible", invisible);
		tag.putBoolean("silent", silent);
		tag.putBoolean("invulnerable", invulnerable);
		tag.putBoolean("no_ai", noAi);

		if (targetId != null) {
			tag.putIntArray("target", UUIDUtil.uuidToIntArray(targetId));
		}

		ListTag effectList = new ListTag();
		effectList.addAll(effects);
		tag.put("effects", effectList);

		return tag;
	}

	public static SuppressedMobState fromNbt(CompoundTag tag) {
		Identifier dimensionId = Identifier.tryParse(tag.getStringOr("dimension", ""));

		if (dimensionId == null) {
			throw new IllegalArgumentException("Suppressed mob state has an unreadable dimension");
		}

		List<CompoundTag> effects = new ArrayList<>();

		for (Tag element : tag.getListOrEmpty("effects")) {
			effects.add((CompoundTag) element);
		}

		return new SuppressedMobState(
				UUIDUtil.uuidFromIntArray(tag.getIntArray("entity").orElse(new int[4])),
				ResourceKey.create(Registries.DIMENSION, dimensionId),
				new Vec3(tag.getDoubleOr("x", 0.0D), tag.getDoubleOr("y", 0.0D), tag.getDoubleOr("z", 0.0D)),
				new Vec3(tag.getDoubleOr("vx", 0.0D), tag.getDoubleOr("vy", 0.0D), tag.getDoubleOr("vz", 0.0D)),
				tag.getFloatOr("yaw", 0.0F),
				tag.getFloatOr("pitch", 0.0F),
				tag.getFloatOr("health", 0.0F),
				tag.getIntOr("fire", 0),
				tag.getIntOr("freeze", 0),
				tag.getBooleanOr("invisible", false),
				tag.getBooleanOr("silent", false),
				tag.getBooleanOr("invulnerable", false),
				tag.getBooleanOr("no_ai", false),
				tag.contains("target") ? UUIDUtil.uuidFromIntArray(tag.getIntArray("target").orElse(new int[4])) : null,
				List.copyOf(effects));
	}

	public static SuppressedMobState placeholderFor(UUID id, ResourceKey<Level> dimension) {
		return new SuppressedMobState(id, dimension, Vec3.ZERO, Vec3.ZERO, 0.0F, 0.0F, 0.0F, 0, 0,
				false, false, false, false, null, List.of());
	}

	public static UUID readEntityId(CompoundTag tag) {
		return UUIDUtil.uuidFromIntArray(((IntArrayTag) tag.get("entity")).getAsIntArray());
	}
}
