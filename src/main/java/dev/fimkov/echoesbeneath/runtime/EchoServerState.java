package dev.fimkov.echoesbeneath.runtime;

import dev.fimkov.echoesbeneath.chart.EchoChartService;
import dev.fimkov.echoesbeneath.memory.storage.EchoMemoryStorage;
import dev.fimkov.echoesbeneath.playback.EchoPlaybackManager;
import dev.fimkov.echoesbeneath.playback.suppression.HostileMobSuppressionManager;
import dev.fimkov.echoesbeneath.recording.EchoRecordingManager;
import net.minecraft.server.MinecraftServer;

public final class EchoServerState {
	private static volatile EchoServerState current;

	private final MinecraftServer server;
	private final EchoMemoryStorage storage;
	private final EchoChartService charts;
	private final HostileMobSuppressionManager suppression;
	private final EchoRecordingManager recordings;
	private final EchoPlaybackManager playbacks;

	private EchoServerState(MinecraftServer server) {
		this.server = server;
		this.storage = EchoMemoryStorage.forServer(server);
		this.charts = new EchoChartService(storage);
		this.suppression = new HostileMobSuppressionManager(server);
		this.recordings = new EchoRecordingManager(this);
		this.playbacks = new EchoPlaybackManager(this);
	}

	public static EchoServerState current() {
		return current;
	}

	public static EchoServerState attach(MinecraftServer server) {
		EchoServerState state = new EchoServerState(server);
		current = state;
		return state;
	}

	public static void detach() {
		current = null;
	}

	public MinecraftServer server() {
		return server;
	}

	public EchoChartService charts() {
		return charts;
	}

	public EchoMemoryStorage storage() {
		return storage;
	}

	public HostileMobSuppressionManager suppression() {
		return suppression;
	}

	public EchoRecordingManager recordings() {
		return recordings;
	}

	public EchoPlaybackManager playbacks() {
		return playbacks;
	}

	public void tick() {
		recordings().tick();
		playbacks().tick();
	}
}
