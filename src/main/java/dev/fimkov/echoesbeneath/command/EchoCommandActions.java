package dev.fimkov.echoesbeneath.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.item.EchoLensStacks;
import dev.fimkov.echoesbeneath.memory.EchoMemoryLocator;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReferences;
import dev.fimkov.echoesbeneath.memory.EchoTextFormat;
import dev.fimkov.echoesbeneath.playback.EchoPlaybackSession;
import dev.fimkov.echoesbeneath.playback.EchoPlaybackStopReason;
import dev.fimkov.echoesbeneath.recording.EchoRecordingSession;
import dev.fimkov.echoesbeneath.recording.EchoRecordingStopReason;
import dev.fimkov.echoesbeneath.registry.EchoItems;
import dev.fimkov.echoesbeneath.runtime.EchoServerState;
import dev.fimkov.echoesbeneath.worldgen.EchoStructures;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;
import java.util.UUID;

public final class EchoCommandActions {
	private static final int SUCCESS = 1;
	private static final int FAILURE = 0;
	private static final int STRUCTURE_SEARCH_RADIUS_CHUNKS = 100;

	private EchoCommandActions() {
	}

	public static int give(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		ItemStack lens = new ItemStack(EchoItems.ECHO_LENS);
		if (!player.getInventory().add(lens)) {
			player.drop(lens, false);
		}
		context.getSource().sendSuccess(() -> Component.translatable(EchoTranslations.ITEM_ECHO_LENS), false);
		return SUCCESS;
	}

	public static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		ItemStack held = player.getMainHandItem();
		if (!EchoLensStacks.isLens(held)) {
			context.getSource().sendFailure(Component.translatable(EchoTranslations.COMMAND_NO_LENS));
			return FAILURE;
		}
		EchoMemoryReferences references = EchoLensStacks.references(held);
		UUID identity = EchoLensStacks.identityOrNull(held);
		context.getSource().sendSuccess(() -> Component.translatable(EchoTranslations.COMMAND_STATUS_HEADER,
				identity == null ? "-" : identity.toString(),
				references.filledSlots(),
				dev.fimkov.echoesbeneath.memory.EchoFormat.MEMORY_SLOT_COUNT), false);
		for (EchoMemoryReference reference : references.entries()) {
			context.getSource().sendSuccess(() -> Component.translatable(EchoTranslations.COMMAND_STATUS_ENTRY,
					EchoTextFormat.romanSlot(reference.slot()),
					reference.memoryId().toString(),
					reference.center().getX(), reference.center().getY(), reference.center().getZ(),
					reference.radius(),
					EchoTextFormat.clock(reference.durationTicks())), false);
		}
		return SUCCESS;
	}

	public static int startRecording(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		EchoServerState state = requireState(context);
		if (state == null) {
			return FAILURE;
		}
		ItemStack held = player.getMainHandItem();
		if (!EchoLensStacks.isLens(held)) {
			context.getSource().sendFailure(Component.translatable(EchoTranslations.COMMAND_NO_LENS));
			return FAILURE;
		}
		BlockPos support = player.blockPosition().below();
		return state.recordings().start(player, held, support).isPresent() ? SUCCESS : FAILURE;
	}

	public static int stopRecording(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		EchoServerState state = requireState(context);
		if (state == null) {
			return FAILURE;
		}
		Optional<EchoRecordingSession> session = state.recordings().sessionOwnedBy(player.getUUID());
		if (session.isEmpty()) {
			context.getSource().sendFailure(Component.translatable(EchoTranslations.COMMAND_NOT_RECORDING));
			return FAILURE;
		}
		state.recordings().stop(session.get(), EchoRecordingStopReason.OWNER_REQUEST);
		return SUCCESS;
	}

	public static int playNearby(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		EchoServerState state = requireState(context);
		if (state == null) {
			return FAILURE;
		}
		ItemStack held = player.getMainHandItem();
		if (!EchoLensStacks.isLens(held)) {
			context.getSource().sendFailure(Component.translatable(EchoTranslations.COMMAND_NO_LENS));
			return FAILURE;
		}
		Optional<EchoMemoryReference> reference = EchoMemoryLocator.resonating(
				EchoLensStacks.references(held), player.level().dimension(), player.blockPosition());
		if (reference.isEmpty()) {
			context.getSource().sendFailure(Component.translatable(EchoTranslations.MESSAGE_NO_RESONANCE));
			return FAILURE;
		}
		return state.playbacks().start(player, held, reference.get()) ? SUCCESS : FAILURE;
	}

	public static int stopPlayback(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		EchoServerState state = requireState(context);
		if (state == null) {
			return FAILURE;
		}
		Optional<EchoPlaybackSession> session = state.playbacks().sessionOwnedBy(player.getUUID());
		if (session.isEmpty()) {
			context.getSource().sendFailure(Component.translatable(EchoTranslations.COMMAND_NOT_PLAYING));
			return FAILURE;
		}
		state.playbacks().stop(session.get(), EchoPlaybackStopReason.OWNER_REQUEST);
		return SUCCESS;
	}

	public static int debugBounds(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		EchoServerState state = requireState(context);
		if (state == null) {
			return FAILURE;
		}
		Optional<EchoPlaybackSession> playback = state.playbacks()
				.sessionAt(player.serverLevel(), player.position());
		if (playback.isPresent()) {
			EchoPlaybackSession session = playback.get();
			context.getSource().sendSuccess(() -> Component.translatable(EchoTranslations.COMMAND_DEBUG_BOUNDS,
					session.region().center().getX(), session.region().center().getY(),
					session.region().center().getZ(), session.region().radius(),
					session.region().verticalRadius(), session.currentTick(),
					session.record().durationTicks()), false);
			return SUCCESS;
		}
		Optional<EchoRecordingSession> recording = state.recordings().sessionOwnedBy(player.getUUID());
		if (recording.isEmpty()) {
			context.getSource().sendFailure(Component.translatable(EchoTranslations.COMMAND_NOT_PLAYING));
			return FAILURE;
		}
		EchoRecordingSession session = recording.get();
		context.getSource().sendSuccess(() -> Component.translatable(EchoTranslations.COMMAND_DEBUG_BOUNDS,
				session.center().getX(), session.center().getY(), session.center().getZ(),
				dev.fimkov.echoesbeneath.config.EchoConfig.values().recordingRadius(),
				dev.fimkov.echoesbeneath.config.EchoConfig.values().recordingVerticalRadius(),
				session.currentTick(), session.maxTicks()), false);
		return SUCCESS;
	}

	public static int debugSounds(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		EchoServerState state = requireState(context);
		if (state == null) {
			return FAILURE;
		}
		Optional<EchoPlaybackSession> playback = state.playbacks()
				.sessionAt(player.serverLevel(), player.position());
		if (playback.isPresent()) {
			EchoPlaybackSession session = playback.get();
			context.getSource().sendSuccess(() -> Component.translatable(EchoTranslations.COMMAND_DEBUG_SOUNDS,
					session.record().soundTimeline().size(), session.currentTick()), false);
			return SUCCESS;
		}
		Optional<EchoRecordingSession> recording = state.recordings().sessionOwnedBy(player.getUUID());
		if (recording.isEmpty()) {
			context.getSource().sendFailure(Component.translatable(EchoTranslations.COMMAND_NOT_RECORDING));
			return FAILURE;
		}
		context.getSource().sendSuccess(() -> Component.translatable(EchoTranslations.COMMAND_DEBUG_SOUNDS,
				recording.get().recordedSoundCount(), recording.get().currentTick()), false);
		return SUCCESS;
	}

	public static int debugEntities(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		EchoServerState state = requireState(context);
		if (state == null) {
			return FAILURE;
		}
		Optional<EchoPlaybackSession> playback = state.playbacks()
				.sessionAt(player.serverLevel(), player.position());
		if (playback.isPresent()) {
			EchoPlaybackSession session = playback.get();
			context.getSource().sendSuccess(() -> Component.translatable(EchoTranslations.COMMAND_DEBUG_ENTITIES,
					session.record().entityTimeline().trackCount(),
					session.record().entityTimeline().totalKeyframes()), false);
			return SUCCESS;
		}
		Optional<EchoRecordingSession> recording = state.recordings().sessionOwnedBy(player.getUUID());
		if (recording.isEmpty()) {
			context.getSource().sendFailure(Component.translatable(EchoTranslations.COMMAND_NOT_RECORDING));
			return FAILURE;
		}
		context.getSource().sendSuccess(() -> Component.translatable(EchoTranslations.COMMAND_DEBUG_ENTITIES,
				recording.get().recordedEntityCount(), recording.get().recordedBlockEventCount()), false);
		return SUCCESS;
	}

	public static int recoverSuppressedMobs(CommandContext<CommandSourceStack> context) {
		EchoServerState state = requireState(context);
		if (state == null) {
			return FAILURE;
		}
		int restored = state.suppression().recoverEverything();
		context.getSource().sendSuccess(() ->
				Component.translatable(EchoTranslations.COMMAND_RECOVERED_MOBS, restored), true);
		return SUCCESS;
	}

	public static int locateStructure(CommandContext<CommandSourceStack> context, String structureName)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		ServerLevel level = player.serverLevel();
		Optional<Holder.Reference<Structure>> structure = level.registryAccess()
				.registryOrThrow(Registries.STRUCTURE)
				.getHolder(EchoStructures.structureKey(structureName));
		if (structure.isEmpty()) {
			context.getSource().sendFailure(
					Component.translatable(EchoTranslations.COMMAND_STRUCTURE_MISSING, structureName));
			return FAILURE;
		}
		Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator().findNearestMapStructure(
				level, HolderSet.direct(structure.get()), player.blockPosition(),
				STRUCTURE_SEARCH_RADIUS_CHUNKS, false);
		if (found == null) {
			context.getSource().sendFailure(
					Component.translatable(EchoTranslations.COMMAND_STRUCTURE_MISSING, structureName));
			return FAILURE;
		}
		BlockPos position = found.getFirst();
		int distance = (int) Math.sqrt(player.blockPosition().distSqr(position));
		context.getSource().sendSuccess(() -> Component.translatable(EchoTranslations.COMMAND_STRUCTURE_FOUND,
				structureName, position.getX(), position.getY(), position.getZ(), distance), false);
		return SUCCESS;
	}

	public static int adminClearMemory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		EchoServerState state = requireState(context);
		if (state == null) {
			return FAILURE;
		}
		String rawMemory = StringArgumentType.getString(context, EchoCommands.MEMORY_ARGUMENT);
		ItemStack held = player.getMainHandItem();
		if (!EchoLensStacks.isLens(held)) {
			context.getSource().sendFailure(Component.translatable(EchoTranslations.COMMAND_NO_LENS));
			return FAILURE;
		}
		Optional<UUID> memoryId = parseMemoryId(EchoLensStacks.references(held), rawMemory);
		if (memoryId.isEmpty()) {
			context.getSource().sendFailure(Component.translatable(EchoTranslations.MESSAGE_MEMORY_CORRUPT));
			return FAILURE;
		}
		state.storage().delete(memoryId.get());
		EchoLensStacks.setReferences(held, EchoLensStacks.references(held).withRemoved(memoryId.get()));
		context.getSource().sendSuccess(() ->
				Component.translatable(EchoTranslations.COMMAND_MEMORY_CLEARED, rawMemory), true);
		return SUCCESS;
	}

	private static Optional<UUID> parseMemoryId(EchoMemoryReferences references, String raw) {
		try {
			int slot = Integer.parseInt(raw) - 1;
			return references.findBySlot(slot).map(EchoMemoryReference::memoryId);
		} catch (NumberFormatException notASlot) {
			try {
				UUID parsed = UUID.fromString(raw);
				return references.findById(parsed).map(EchoMemoryReference::memoryId);
			} catch (IllegalArgumentException notAUuid) {
				return Optional.empty();
			}
		}
	}

	private static EchoServerState requireState(CommandContext<CommandSourceStack> context) {
		EchoServerState state = EchoServerState.current();
		if (state == null) {
			context.getSource().sendFailure(Component.translatable(EchoTranslations.COMMAND_NOT_RECORDING));
		}
		return state;
	}
}
