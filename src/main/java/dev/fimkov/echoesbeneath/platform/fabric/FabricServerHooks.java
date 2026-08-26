package dev.fimkov.echoesbeneath.platform.fabric;

import com.mojang.brigadier.CommandDispatcher;
import dev.fimkov.echoesbeneath.platform.EchoServerHooks;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class FabricServerHooks implements EchoServerHooks {
	@Override
	public void onServerStarted(Consumer<MinecraftServer> handler) {
		ServerLifecycleEvents.SERVER_STARTED.register(handler::accept);
	}

	@Override
	public void onServerStopping(Consumer<MinecraftServer> handler) {
		ServerLifecycleEvents.SERVER_STOPPING.register(handler::accept);
	}

	@Override
	public void onServerStopped(Consumer<MinecraftServer> handler) {
		ServerLifecycleEvents.SERVER_STOPPED.register(handler::accept);
	}

	@Override
	public void onServerTickEnd(Consumer<MinecraftServer> handler) {
		ServerTickEvents.END_SERVER_TICK.register(handler::accept);
	}

	@Override
	public void onLevelLoad(BiConsumer<MinecraftServer, ServerLevel> handler) {
		ServerWorldEvents.LOAD.register(handler::accept);
	}

	@Override
	public void onLevelUnload(BiConsumer<MinecraftServer, ServerLevel> handler) {
		ServerWorldEvents.UNLOAD.register(handler::accept);
	}

	@Override
	public void onEntityLoad(BiConsumer<Entity, ServerLevel> handler) {
		ServerEntityEvents.ENTITY_LOAD.register(handler::accept);
	}

	@Override
	public void onEntityChangedLevel(BiConsumer<Entity, ServerLevel> handler) {
		ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.register(
				(originalEntity, newEntity, origin, destination) -> handler.accept(newEntity, origin));
	}

	@Override
	public void onPlayerDisconnect(Consumer<ServerPlayer> handler) {
		ServerPlayConnectionEvents.DISCONNECT.register((networkHandler, server) ->
				handler.accept(networkHandler.player));
	}

	@Override
	public void onCommandRegistration(Consumer<CommandDispatcher<CommandSourceStack>> handler) {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
				handler.accept(dispatcher));
	}

	@Override
	public void guardBlockBreak(BlockBreakGuard guard) {
		PlayerBlockBreakEvents.BEFORE.register((level, player, position, state, blockEntity) ->
				guard.allow(level, player, position));
	}

	@Override
	public void guardUseBlock(UseBlockGuard guard) {
		UseBlockCallback.EVENT.register((player, level, hand, hit) ->
				guard.allow(player, level, hand, hit) ? InteractionResult.PASS : InteractionResult.FAIL);
	}

	@Override
	public void guardUseItem(UseItemGuard guard) {
		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			return guard.allow(player, level, hand)
					? InteractionResultHolder.pass(stack)
					: InteractionResultHolder.fail(stack);
		});
	}

	@Override
	public void guardAttack(AttackGuard guard) {
		AttackEntityCallback.EVENT.register((player, level, hand, target, hit) ->
				guard.allow(player, target) ? InteractionResult.PASS : InteractionResult.FAIL);
	}
}
