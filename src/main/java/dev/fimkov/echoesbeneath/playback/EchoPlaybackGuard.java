package dev.fimkov.echoesbeneath.playback;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.config.EchoConfig;
import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import dev.fimkov.echoesbeneath.runtime.EchoServerState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class EchoPlaybackGuard {
	private EchoPlaybackGuard() {
	}

	public static void register() {
		EchoPlatforms.server().guardBlockBreak((level, player, position) ->
				!isBlocked(level, player, position));

		EchoPlatforms.server().guardUseBlock((player, level, hand, hitResult) ->
				!isBlocked(level, player, hitResult.getBlockPos()));

		EchoPlatforms.server().guardUseItem((player, level, hand) -> {
			ItemStack stack = player.getItemInHand(hand);

			return !(stack.getItem() instanceof BucketItem
					&& isBlocked(level, player, player.blockPosition()));
		});

		EchoPlatforms.server().guardAttack((player, target) -> {
			EchoServerState state = EchoServerState.current();

			return state == null || !state.playbacks().protectsEntity(target);
		});
	}

	private static boolean isBlocked(Level level, Player player, BlockPos position) {
		if (level.isClientSide() || !EchoConfig.values().disableBlockInteractionDuringPlayback()) {
			return false;
		}

		if (!(level instanceof ServerLevel serverLevel)) {
			return false;
		}

		EchoServerState state = EchoServerState.current();

		if (state == null || !state.playbacks().isInsideAnyRegion(serverLevel, position)) {
			return false;
		}

		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendOverlayMessage(
					Component.translatable(EchoTranslations.MESSAGE_PLAYBACK_BLOCKED_INTERACTION));
		}

		return true;
	}
}
