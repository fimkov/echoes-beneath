package dev.fimkov.echoesbeneath.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import dev.fimkov.echoesbeneath.worldgen.EchoStructures;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

public final class EchoCommands {
	public static final int OPERATOR_LEVEL = 2;
	public static final int RECOVERY_LEVEL = 3;
	public static final String STRUCTURE_ARGUMENT = "type";
	public static final String MEMORY_ARGUMENT = "memory";

	private EchoCommands() {
	}

	public static void register() {
		EchoPlatforms.server().onCommandRegistration(dispatcher -> dispatcher.register(buildRoot()));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRoot() {
		return Commands.literal("echo")
				.requires(Commands.LEVEL_GAMEMASTERS)
				.then(Commands.literal("give").executes(EchoCommandActions::give))
				.then(Commands.literal("status").executes(EchoCommandActions::status))
				.then(Commands.literal("start_recording").executes(EchoCommandActions::startRecording))
				.then(Commands.literal("stop_recording").executes(EchoCommandActions::stopRecording))
				.then(Commands.literal("play_nearby").executes(EchoCommandActions::playNearby))
				.then(Commands.literal("stop_playback").executes(EchoCommandActions::stopPlayback))
				.then(Commands.literal("debug_bounds").executes(EchoCommandActions::debugBounds))
				.then(Commands.literal("debug_sounds").executes(EchoCommandActions::debugSounds))
				.then(Commands.literal("debug_entities").executes(EchoCommandActions::debugEntities))
				.then(Commands.literal("recover_suppressed_mobs").executes(EchoCommandActions::recoverSuppressedMobs))
				.then(Commands.literal("locate_structure")
						.then(Commands.argument(STRUCTURE_ARGUMENT, StringArgumentType.word())
								.suggests((context, builder) ->
										SharedSuggestionProvider.suggest(EchoStructures.ALL, builder))
								.executes(EchoCommands::locateStructure)))
				.then(Commands.literal("admin_clear_memory")
						.requires(Commands.LEVEL_ADMINS)
						.then(Commands.argument(MEMORY_ARGUMENT, StringArgumentType.word())
								.executes(EchoCommandActions::adminClearMemory)));
	}

	private static int locateStructure(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		return EchoCommandActions.locateStructure(context, StringArgumentType.getStringOr(context, STRUCTURE_ARGUMENT, ""));
	}
}
