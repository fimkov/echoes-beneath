package dev.fimkov.echoesbeneath.playback;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.config.EchoConfig;
import dev.fimkov.echoesbeneath.runtime.EchoServerState;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class EchoPlaybackGuard {
	private EchoPlaybackGuard() {
	}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, position, blockState, blockEntity) ->
				!isBlocked(level, player, position));

		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (isBlocked(level, player, hitResult.getBlockPos())) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});

		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof BucketItem && isBlocked(level, player, player.blockPosition())) {
				return InteractionResultHolder.fail(stack);
			}
			return InteractionResultHolder.pass(stack);
		});

		AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
			EchoServerState state = EchoServerState.current();
			if (state != null && state.playbacks().protectsEntity(target)) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
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
			serverPlayer.displayClientMessage(
					Component.translatable(EchoTranslations.MESSAGE_PLAYBACK_BLOCKED_INTERACTION), true);
		}
		return true;
	}
}
