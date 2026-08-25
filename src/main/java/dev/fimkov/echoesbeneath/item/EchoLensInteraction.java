package dev.fimkov.echoesbeneath.item;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.memory.EchoMemoryLocator;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReferences;
import dev.fimkov.echoesbeneath.runtime.EchoServerState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import java.util.Optional;

public final class EchoLensInteraction {
	private EchoLensInteraction() {
	}

	public static InteractionResult useOn(UseOnContext context) {
		if (context.getLevel().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (context.getClickedFace() != Direction.UP) {
			return InteractionResult.PASS;
		}
		if (!(context.getPlayer() instanceof ServerPlayer player)) {
			return InteractionResult.PASS;
		}
		EchoServerState state = EchoServerState.current();
		if (state == null) {
			return InteractionResult.PASS;
		}
		ItemStack stack = context.getItemInHand();
		EchoMemoryReferences references = EchoLensStacks.references(stack);
		Optional<EchoMemoryReference> resonating = EchoMemoryLocator.resonating(
				references, player.level().dimension(), player.blockPosition());
		if (resonating.isPresent()) {
			return startPlayback(state, player, context.getHand(), resonating.get());
		}
		return startRecording(state, player, context.getHand(), context.getClickedPos(), references);
	}

	private static InteractionResult startPlayback(EchoServerState state, ServerPlayer player,
			net.minecraft.world.InteractionHand hand, EchoMemoryReference reference) {
		return state.playbacks().start(player, hand, reference)
				? InteractionResult.CONSUME
				: InteractionResult.FAIL;
	}

	private static InteractionResult startRecording(EchoServerState state, ServerPlayer player,
			net.minecraft.world.InteractionHand hand, BlockPos clickedPos, EchoMemoryReferences references) {
		if (references.isFull()) {
			player.displayClientMessage(Component.translatable(EchoTranslations.MESSAGE_NO_MORE), true);
			return InteractionResult.FAIL;
		}
		return state.recordings().start(player, hand, clickedPos).isPresent()
				? InteractionResult.CONSUME
				: InteractionResult.FAIL;
	}
}
