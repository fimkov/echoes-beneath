package dev.fimkov.echoesbeneath.playback;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.advancement.EchoCriteria;
import dev.fimkov.echoesbeneath.config.EchoConfig;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockSnapshot;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class EchoMending {
	private static final float PLACE_VOLUME = 0.7F;
	private static final float PLACE_PITCH = 1.15F;
	private static final int PARTICLE_COUNT = 6;
	private static final double PARTICLE_SPREAD = 0.28D;
	private static final int PLACE_FLAGS = 3;

	private EchoMending() {
	}

	public static boolean tryMend(ServerLevel level, ServerPlayer player, BlockPos target,
			InteractionHand hand) {
		if (!EchoConfig.values().allowMemoryMending()) {
			return false;
		}
		EchoPlaybackSession session = sessionHolding(level, target);
		if (session == null) {
			return false;
		}
		BlockState remembered = rememberedAt(session, target);
		if (remembered == null || !isRestorable(remembered)) {
			return false;
		}
		if (!level.getBlockState(target).canBeReplaced() || !remembered.canSurvive(level, target)) {
			return false;
		}
		if (!remembered.getCollisionShape(level, target).isEmpty()
				&& !level.getEntities((net.minecraft.world.entity.Entity) null,
						new AABB(target)).isEmpty()) {
			return false;
		}
		Item needed = remembered.getBlock().asItem();
		if (needed == Items.AIR) {
			return false;
		}
		int cost = EchoConfig.values().mendingExperienceCost();
		if (!player.getAbilities().instabuild) {
			if (cost > 0 && player.experienceLevel <= 0 && player.experienceProgress <= 0.0F) {
				player.displayClientMessage(
						Component.translatable(EchoTranslations.MESSAGE_MENDING_NO_EXPERIENCE), true);
				return false;
			}
			int slot = findMaterial(player, needed);
			if (slot < 0) {
				player.displayClientMessage(Component.translatable(
						EchoTranslations.MESSAGE_MENDING_MISSING, needed.getDescription()), true);
				return false;
			}
			player.getInventory().removeItem(slot, 1);
			player.giveExperiencePoints(-cost);
		}
		level.setBlock(target, remembered, PLACE_FLAGS);
		level.playSound(null, target, EchoSounds.RESONANCE_PING, SoundSource.BLOCKS,
				PLACE_VOLUME, PLACE_PITCH);
		level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX() + 0.5D, target.getY() + 0.5D,
				target.getZ() + 0.5D, PARTICLE_COUNT, PARTICLE_SPREAD, PARTICLE_SPREAD,
				PARTICLE_SPREAD, 0.0D);
		EchoCriteria.MEMORY_MENDED.trigger(player, remembered);
		player.swing(hand, true);
		return true;
	}

	private static EchoPlaybackSession sessionHolding(ServerLevel level, BlockPos target) {
		dev.fimkov.echoesbeneath.runtime.EchoServerState state =
				dev.fimkov.echoesbeneath.runtime.EchoServerState.current();
		if (state == null) {
			return null;
		}
		for (EchoPlaybackSession session : state.playbacks().allSessions()) {
			if (session.level() == level && session.region().contains(level.dimension(),
					net.minecraft.world.phys.Vec3.atCenterOf(target))) {
				return session;
			}
		}
		return null;
	}

	public static boolean isRestorable(BlockState remembered) {
		if (remembered == null || remembered.isAir()) {
			return false;
		}
		if (!remembered.getFluidState().isEmpty()) {
			return false;
		}
		if (remembered.hasBlockEntity()) {
			return false;
		}
		return remembered.getBlock().asItem() != Items.AIR;
	}

	private static BlockState rememberedAt(EchoPlaybackSession session, BlockPos target) {
		BlockPos center = session.region().center();
		BlockSnapshot snapshot = session.record().snapshot();
		int relativeX = target.getX() - center.getX();
		int relativeY = target.getY() - center.getY();
		int relativeZ = target.getZ() - center.getZ();
		if (Math.abs(relativeX) > snapshot.radius() || Math.abs(relativeZ) > snapshot.radius()
				|| relativeY < snapshot.minRelativeY() || relativeY > snapshot.maxRelativeY()) {
			return null;
		}
		BlockState state = snapshot.stateAt(relativeX, relativeY, relativeZ);
		return state;
	}

	private static int findMaterial(ServerPlayer player, Item needed) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack candidate = player.getInventory().getItem(slot);
			if (candidate.is(needed)) {
				return slot;
			}
		}
		return -1;
	}
}
