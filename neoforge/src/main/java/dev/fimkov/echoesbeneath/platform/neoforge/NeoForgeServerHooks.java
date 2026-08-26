package dev.fimkov.echoesbeneath.platform.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import dev.fimkov.echoesbeneath.platform.EchoServerHooks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class NeoForgeServerHooks implements EchoServerHooks {
	private final IEventBus bus = NeoForge.EVENT_BUS;

	@Override
	public void onServerStarted(Consumer<MinecraftServer> handler) {
		bus.addListener(ServerStartedEvent.class, event -> handler.accept(event.getServer()));
	}

	@Override
	public void onServerStopping(Consumer<MinecraftServer> handler) {
		bus.addListener(ServerStoppingEvent.class, event -> handler.accept(event.getServer()));
	}

	@Override
	public void onServerStopped(Consumer<MinecraftServer> handler) {
		bus.addListener(ServerStoppedEvent.class, event -> handler.accept(event.getServer()));
	}

	@Override
	public void onServerTickEnd(Consumer<MinecraftServer> handler) {
		bus.addListener(ServerTickEvent.Post.class, event -> handler.accept(event.getServer()));
	}

	@Override
	public void onLevelLoad(BiConsumer<MinecraftServer, ServerLevel> handler) {
		bus.addListener(LevelEvent.Load.class, event -> {
			if (event.getLevel() instanceof ServerLevel level) {
				handler.accept(level.getServer(), level);
			}
		});
	}

	@Override
	public void onLevelUnload(BiConsumer<MinecraftServer, ServerLevel> handler) {
		bus.addListener(LevelEvent.Unload.class, event -> {
			if (event.getLevel() instanceof ServerLevel level) {
				handler.accept(level.getServer(), level);
			}
		});
	}

	@Override
	public void onEntityLoad(BiConsumer<Entity, ServerLevel> handler) {
		bus.addListener(EntityJoinLevelEvent.class, event -> {
			if (event.getLevel() instanceof ServerLevel level) {
				handler.accept(event.getEntity(), level);
			}
		});
	}

	@Override
	public void onStartTracking(BiConsumer<Entity, ServerPlayer> handler) {
		bus.addListener(PlayerEvent.StartTracking.class, event -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				handler.accept(event.getTarget(), player);
			}
		});
	}

	@Override
	public void onEntityChangedLevel(BiConsumer<Entity, ServerLevel> handler) {
		onEntityLoad(handler);
	}

	@Override
	public void onPlayerDisconnect(Consumer<ServerPlayer> handler) {
		bus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				handler.accept(player);
			}
		});
	}

	@Override
	public void onCommandRegistration(Consumer<CommandDispatcher<CommandSourceStack>> handler) {
		bus.addListener(RegisterCommandsEvent.class, event -> handler.accept(event.getDispatcher()));
	}

	@Override
	public void guardBlockBreak(BlockBreakGuard guard) {
		bus.addListener(BlockEvent.BreakEvent.class, event -> {
			if (event.getLevel() instanceof net.minecraft.world.level.Level level
					&& !guard.allow(level, event.getPlayer(), event.getPos())) {
				event.setCanceled(true);
			}
		});
	}

	@Override
	public void guardUseBlock(UseBlockGuard guard) {
		bus.addListener(PlayerInteractEvent.RightClickBlock.class, event -> {
			if (!guard.allow(event.getEntity(), event.getLevel(), event.getHand(),
					event.getHitVec())) {
				event.setCanceled(true);
			}
		});
	}

	@Override
	public void guardUseItem(UseItemGuard guard) {
		bus.addListener(PlayerInteractEvent.RightClickItem.class, event -> {
			if (!guard.allow(event.getEntity(), event.getLevel(), event.getHand())) {
				event.setCanceled(true);
			}
		});
	}

	@Override
	public void guardAttack(AttackGuard guard) {
		bus.addListener(AttackEntityEvent.class, event -> {
			if (!guard.allow(event.getEntity(), event.getTarget())) {
				event.setCanceled(true);
			}
		});
	}
}
