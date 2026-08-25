package dev.fimkov.echoesbeneath.runtime;

import dev.fimkov.echoesbeneath.playback.EchoPlaybackGuard;
import dev.fimkov.echoesbeneath.playback.EchoPlaybackStopReason;
import dev.fimkov.echoesbeneath.recording.EchoRecordingHooks;
import dev.fimkov.echoesbeneath.recording.EchoRecordingStopReason;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class EchoServerLifecycle {
	private EchoServerLifecycle() {
	}

	public static void register() {
		EchoPlaybackGuard.register();

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			EchoServerState state = EchoServerState.attach(server);
			state.suppression().recoverEverything();
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			EchoServerState state = EchoServerState.current();
			if (state != null) {
				state.recordings().stopAll(EchoRecordingStopReason.SERVER_STOPPING);
				state.playbacks().stopAll(EchoPlaybackStopReason.SERVER_STOPPING);
				state.suppression().recoverEverything();
			}
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			EchoRecordingHooks.setAnyRecordingActive(false);
			EchoServerState.detach();
		});

		ServerWorldEvents.UNLOAD.register((server, level) -> {
			EchoServerState state = EchoServerState.current();
			if (state != null) {
				state.recordings().stopIn(level, EchoRecordingStopReason.DIMENSION_UNLOADED);
				state.playbacks().stopIn(level, EchoPlaybackStopReason.DIMENSION_UNLOADED);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			EchoServerState state = EchoServerState.current();
			if (state != null) {
				state.tick();
			}
		});

		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			EchoServerState state = EchoServerState.current();
			if (state != null) {
				state.suppression().restoreOnLoad(entity);
			}
		});

		ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.register((original, copy, origin, destination) -> {
			EchoServerState state = EchoServerState.current();
			if (state != null) {
				state.suppression().restoreOnLoad(copy);
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			EchoServerState state = EchoServerState.current();
			if (state != null) {
				state.playbacks().onOwnerDisconnected(handler.getPlayer().getUUID());
			}
		});
	}
}
