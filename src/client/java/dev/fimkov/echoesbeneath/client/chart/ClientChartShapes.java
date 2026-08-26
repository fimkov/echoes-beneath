package dev.fimkov.echoesbeneath.client.chart;

import dev.fimkov.echoesbeneath.chart.EchoChartCloud;
import dev.fimkov.echoesbeneath.chart.EchoChartCodec;
import dev.fimkov.echoesbeneath.network.ChartShapePayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientChartShapes {
	private static final long ASK_INTERVAL_MILLIS = 1500L;

	private static final Map<Long, byte[][]> PENDING = new HashMap<>();
	private static final Map<Long, EchoChartModel> READY = new HashMap<>();
	private static final Map<Long, Long> ASKED = new HashMap<>();

	private ClientChartShapes() {
	}

	public static void forget() {
		PENDING.clear();
		READY.clear();
		ASKED.clear();
	}

	public static void ask(net.minecraft.core.BlockPos position) {
		long key = position.asLong();
		long now = System.currentTimeMillis();
		Long last = ASKED.get(key);
		if (last != null && now - last < ASK_INTERVAL_MILLIS) {
			return;
		}
		ASKED.put(key, now);
		dev.fimkov.echoesbeneath.client.platform.EchoClientPlatforms.get().sendToServer(
				new dev.fimkov.echoesbeneath.network.ChartShapeRequestPayload(position));
	}

	public static void forget(long target) {
		PENDING.remove(target);
		READY.remove(target);
		ASKED.remove(target);
	}

	public static EchoChartModel of(long target) {
		return READY.get(target);
	}

	public static void accept(ChartShapePayload payload) {
		byte[][] chunks = PENDING.computeIfAbsent(payload.target(),
				ignored -> new byte[payload.chunkCount()][]);
		if (chunks.length != payload.chunkCount()) {
			PENDING.remove(payload.target());
			return;
		}
		chunks[payload.chunkIndex()] = payload.chunk();
		for (byte[] chunk : chunks) {
			if (chunk == null) {
				return;
			}
		}
		PENDING.remove(payload.target());
		List<byte[]> ordered = new ArrayList<>(chunks.length);
		for (byte[] chunk : chunks) {
			ordered.add(chunk);
		}
		byte[] joined = EchoChartCodec.join(ordered);
		if (joined.length == 0) {
			READY.remove(payload.target());
			return;
		}
		EchoChartCloud cloud = EchoChartCodec.decode(joined);
		READY.put(payload.target(), EchoChartModel.of(cloud));
	}
}
