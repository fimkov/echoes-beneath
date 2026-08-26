package dev.fimkov.echoesbeneath.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface EchoServerHooks {
	@FunctionalInterface
	interface BlockBreakGuard {
		boolean allow(Level level, Player player, BlockPos position);
	}

	@FunctionalInterface
	interface UseBlockGuard {
		boolean allow(Player player, Level level, InteractionHand hand, BlockHitResult hit);
	}

	@FunctionalInterface
	interface UseItemGuard {
		boolean allow(Player player, Level level, InteractionHand hand);
	}

	@FunctionalInterface
	interface AttackGuard {
		boolean allow(Player player, Entity target);
	}

	void onServerStarted(Consumer<MinecraftServer> handler);

	void onServerStopping(Consumer<MinecraftServer> handler);

	void onServerStopped(Consumer<MinecraftServer> handler);

	void onServerTickEnd(Consumer<MinecraftServer> handler);

	void onLevelLoad(BiConsumer<MinecraftServer, ServerLevel> handler);

	void onLevelUnload(BiConsumer<MinecraftServer, ServerLevel> handler);

	void onEntityLoad(BiConsumer<Entity, ServerLevel> handler);

	void onEntityChangedLevel(BiConsumer<Entity, ServerLevel> handler);

	void onStartTracking(BiConsumer<Entity, ServerPlayer> handler);

	void onPlayerDisconnect(Consumer<ServerPlayer> handler);

	void onCommandRegistration(Consumer<CommandDispatcher<CommandSourceStack>> handler);

	void guardBlockBreak(BlockBreakGuard guard);

	void guardUseBlock(UseBlockGuard guard);

	void guardUseItem(UseItemGuard guard);

	void guardAttack(AttackGuard guard);
}
