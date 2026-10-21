package dev.fimkov.echoesbeneath.item;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.memory.EchoMemoryLocator;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReferences;
import dev.fimkov.echoesbeneath.runtime.EchoServerState;
import dev.fimkov.echoesbeneath.registry.EchoBlocks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public final class EchoLensInteraction {
	private EchoLensInteraction() {
	}

	public static InteractionResult useOn(UseOnContext context) {
		if (context.getLevel().isClientSide()) {
			return InteractionResult.SUCCESS;
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
		if (player.isShiftKeyDown()
				&& context.getLevel().getBlockState(context.getClickedPos()).is(EchoBlocks.SOUNDING_TABLE)) {
			return state.playbacks().startPlaylist(player, context.getHand(), context.getClickedPos().above())
					? InteractionResult.CONSUME : InteractionResult.FAIL;
		}
		Optional<EchoMemoryReference> resonating = EchoMemoryLocator.resonating(
				references, player.level().dimension(), player.blockPosition());

		if (resonating.isPresent()) {
			return startPlayback(state, player, context.getHand(), resonating.get());
		}

		return startRecording(state, player, context.getHand(), supportBlockFor(context), references,
				EchoLensStacks.effectiveUpgrades(stack).memorySlots());
	}

	private static BlockPos supportBlockFor(UseOnContext context) {
		BlockPos clicked = context.getClickedPos();

		if (context.getLevel().getBlockState(clicked).canBeReplaced()) {
			return clicked.below();
		}

		if (context.getClickedFace() != Direction.UP) {
			BlockPos against = clicked.relative(context.getClickedFace());

			if (context.getLevel().getBlockState(against).canBeReplaced()) {
				return against.below();
			}
		}

		return clicked;
	}

	private static InteractionResult startPlayback(EchoServerState state, ServerPlayer player,
			InteractionHand hand, EchoMemoryReference reference) {
		return state.playbacks().start(player, hand, reference)
				? InteractionResult.CONSUME
				: InteractionResult.FAIL;
	}

	private static InteractionResult startRecording(EchoServerState state, ServerPlayer player,
			InteractionHand hand, BlockPos clickedPos, EchoMemoryReferences references,
			int capacity) {
		if (references.isFull(capacity)) {
			player.sendOverlayMessage(Component.translatable(EchoTranslations.MESSAGE_NO_MORE));

			return InteractionResult.FAIL;
		}

		return state.recordings().start(player, hand, clickedPos).isPresent()
				? InteractionResult.CONSUME
				: InteractionResult.FAIL;
	}
}
