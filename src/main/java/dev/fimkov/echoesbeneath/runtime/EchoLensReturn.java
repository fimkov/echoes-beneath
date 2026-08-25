package dev.fimkov.echoesbeneath.runtime;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.entity.EchoLensEntity;
import dev.fimkov.echoesbeneath.entity.EchoLensMode;
import dev.fimkov.echoesbeneath.recording.EchoRecordingHooks;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public final class EchoLensReturn {
	private static final int OWNER_PROTECTION_TICKS = 600;
	private static final float RETURN_VOLUME = 0.6F;
	private static final float RETURN_PITCH = 1.4F;

	private EchoLensReturn() {
	}

	public static boolean hasLiveSession(ServerLevel level, EchoLensEntity lens) {
		EchoServerState state = EchoServerState.current();
		if (state == null) {
			return false;
		}
		Optional<UUID> sessionId = lens.getSessionId();
		if (sessionId.isEmpty()) {
			return false;
		}
		if (lens.getMode() == EchoLensMode.RECORDING) {
			return state.recordings().session(sessionId.get()).isPresent();
		}
		if (lens.getMode() == EchoLensMode.PLAYBACK) {
			return state.playbacks().session(sessionId.get()).isPresent();
		}
		return false;
	}

	public static boolean requestEarlyStop(EchoLensEntity lens, Player player) {
		EchoServerState state = EchoServerState.current();
		if (state == null) {
			return false;
		}
		Optional<UUID> sessionId = lens.getSessionId();
		if (sessionId.isEmpty()) {
			return false;
		}
		if (lens.getMode() == EchoLensMode.RECORDING) {
			return state.recordings().requestEarlyStop(sessionId.get(), player.getUUID());
		}
		if (lens.getMode() == EchoLensMode.PLAYBACK) {
			return state.playbacks().requestEarlyStop(sessionId.get(), player.getUUID());
		}
		return false;
	}

	public static void deliverTo(EchoLensEntity lens, Player owner) {
		ItemStack stack = lens.takeLens();
		if (!stack.isEmpty() && !owner.getInventory().add(stack)) {
			dropForOwner((ServerLevel) owner.level(), stack, owner, owner.getUUID());
		}
		if (owner.level() instanceof ServerLevel serverLevel) {
			EchoRecordingHooks.runWithoutCapture(() -> serverLevel.playSound(null,
					owner.getX(), owner.getY(), owner.getZ(),
					EchoSounds.LENS_RETURN, SoundSource.PLAYERS, RETURN_VOLUME, RETURN_PITCH));
		}
		lens.discard();
	}

	public static void dropAtSelf(EchoLensEntity lens) {
		ItemStack stack = lens.takeLens();
		if (!stack.isEmpty() && lens.level() instanceof ServerLevel serverLevel) {
			ItemEntity dropped = new ItemEntity(serverLevel, lens.getX(), lens.getY(), lens.getZ(), stack);
			dropped.setPickUpDelay(0);
			lens.getOwnerId().ifPresent(dropped::setTarget);
			dropped.setUnlimitedLifetime();
			serverLevel.addFreshEntity(dropped);
			notifyOwner(serverLevel, lens.getOwnerId().orElse(null));
		}
		lens.discard();
	}

	private static void dropForOwner(ServerLevel level, ItemStack stack, Player owner, UUID ownerId) {
		ItemEntity dropped = new ItemEntity(level, owner.getX(), owner.getY(), owner.getZ(), stack);
		dropped.setPickUpDelay(0);
		dropped.setTarget(ownerId);
		dropped.setUnlimitedLifetime();
		level.addFreshEntity(dropped);
	}

	private static void notifyOwner(ServerLevel level, UUID ownerId) {
		if (ownerId == null) {
			return;
		}
		Player owner = level.getServer().getPlayerList().getPlayer(ownerId);
		if (owner != null) {
			owner.displayClientMessage(Component.translatable(EchoTranslations.MESSAGE_LENS_RETURNED_TO_GROUND), true);
		}
	}

	public static int ownerProtectionTicks() {
		return OWNER_PROTECTION_TICKS;
	}
}
