package dev.fimkov.echoesbeneath.runtime;

import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import dev.fimkov.echoesbeneath.playback.EchoPlaybackGuard;
import dev.fimkov.echoesbeneath.playback.EchoPlaybackStopReason;
import dev.fimkov.echoesbeneath.recording.EchoRecordingHooks;
import dev.fimkov.echoesbeneath.recording.EchoRecordingStopReason;

public final class EchoServerLifecycle {
	private EchoServerLifecycle() {
	}

	public static void register() {
		EchoPlaybackGuard.register();

		EchoPlatforms.server().onServerStarted(server -> {
			EchoServerState state = EchoServerState.attach(server);
			state.suppression().recoverEverything();
		});

		EchoPlatforms.server().onServerStopping(server -> {
			EchoServerState state = EchoServerState.current();
			if (state != null) {
				state.recordings().stopAll(EchoRecordingStopReason.SERVER_STOPPING);
				state.playbacks().stopAll(EchoPlaybackStopReason.SERVER_STOPPING);
				state.suppression().recoverEverything();
			}
		});

		EchoPlatforms.server().onServerStopped(server -> {
			EchoRecordingHooks.setAnyRecordingActive(false);
			EchoServerState.detach();
		});

		EchoPlatforms.server().onLevelUnload((server, level) -> {
			EchoServerState state = EchoServerState.current();
			if (state != null) {
				state.recordings().stopIn(level, EchoRecordingStopReason.DIMENSION_UNLOADED);
				state.playbacks().stopIn(level, EchoPlaybackStopReason.DIMENSION_UNLOADED);
			}
		});

		EchoPlatforms.server().onServerTickEnd(server -> {
			EchoServerState state = EchoServerState.current();
			if (state != null) {
				state.tick();
			}
		});

		EchoPlatforms.server().onEntityLoad((entity, level) -> {
			EchoServerState state = EchoServerState.current();
			if (state != null) {
				state.suppression().restoreOnLoad(entity);
			}
		});

		EchoPlatforms.server().onEntityChangedLevel((entity, level) -> {
			EchoServerState state = EchoServerState.current();
			if (state != null) {
				state.suppression().restoreOnLoad(entity);
			}
		});

		EchoPlatforms.server().onPlayerDisconnect(player -> {
			EchoServerState state = EchoServerState.current();
			if (state != null) {
				state.recordings().handOverTo(player);
				state.playbacks().onOwnerDisconnected(player);
			}
		});
	}
}
