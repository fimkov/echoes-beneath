package dev.fimkov.echoesbeneath.recording;

import dev.fimkov.echoesbeneath.runtime.EchoServerState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class EchoRecordingHooks {
	private static volatile boolean anyRecordingActive;
	private static boolean captureSuspended;

	private EchoRecordingHooks() {
	}

	public static void setAnyRecordingActive(boolean active) {
		anyRecordingActive = active;
	}

	public static boolean isCapturing() {
		return anyRecordingActive && !captureSuspended;
	}

	public static void runWithoutCapture(Runnable action) {
		boolean previous = captureSuspended;
		captureSuspended = true;

		try {
			action.run();
		} finally {
			captureSuspended = previous;
		}
	}

	public static void onBlockChanged(Level level, BlockPos position, BlockState previous, BlockState current) {
		if (!isCapturing() || !(level instanceof ServerLevel serverLevel)) {
			return;
		}

		EchoServerState state = EchoServerState.current();

		if (state == null) {
			return;
		}

		for (EchoRecordingSession session : state.recordings().sessionsIn(serverLevel)) {
			session.onBlockChanged(position, previous, current);
		}
	}

	public static void onSound(ServerLevel level, double x, double y, double z, Identifier soundId,
			SoundSource source, float volume, float pitch) {
		if (!isCapturing()) {
			return;
		}

		EchoServerState state = EchoServerState.current();

		if (state == null) {
			return;
		}

		for (EchoRecordingSession session : state.recordings().sessionsIn(level)) {
			session.onSound(x, y, z, soundId, source, volume, pitch);
		}
	}

	public static void onParticle(ServerLevel level, double x, double y, double z, Identifier typeId,
			int count, double spreadX, double spreadY, double spreadZ) {
		if (!isCapturing()) {
			return;
		}

		EchoServerState state = EchoServerState.current();

		if (state == null) {
			return;
		}

		for (EchoRecordingSession session : state.recordings().sessionsIn(level)) {
			session.onParticle(x, y, z, typeId, count, spreadX, spreadY, spreadZ);
		}
	}

	public static void onJukebox(ServerLevel level, BlockPos position, Identifier soundId, boolean playing) {
		EchoServerState state = EchoServerState.current();
		if (!isCapturing() || state == null) {
			return;
		}
		for (EchoRecordingSession session : state.recordings().sessionsIn(level)) {
			session.onJukebox(position, soundId, playing);
		}
	}
}
