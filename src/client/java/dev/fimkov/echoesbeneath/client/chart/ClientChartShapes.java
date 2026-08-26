package dev.fimkov.echoesbeneath.client.chart;

import dev.fimkov.echoesbeneath.chart.EchoChartCloud;
import dev.fimkov.echoesbeneath.chart.EchoChartCodec;
import dev.fimkov.echoesbeneath.network.ChartShapePayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientChartShapes {
	private static final Map<Integer, byte[][]> PENDING = new HashMap<>();
	private static final Map<Integer, EchoChartModel> READY = new HashMap<>();

	private ClientChartShapes() {
	}

	public static void forget() {
		PENDING.clear();
		READY.clear();
	}

	public static void forget(int entityId) {
		PENDING.remove(entityId);
		READY.remove(entityId);
	}

	public static EchoChartModel of(int entityId) {
		return READY.get(entityId);
	}

	public static void accept(ChartShapePayload payload) {
		byte[][] chunks = PENDING.computeIfAbsent(payload.entityId(),
				ignored -> new byte[payload.chunkCount()][]);
		if (chunks.length != payload.chunkCount()) {
			PENDING.remove(payload.entityId());
			return;
		}
		chunks[payload.chunkIndex()] = payload.chunk();
		for (byte[] chunk : chunks) {
			if (chunk == null) {
				return;
			}
		}
		PENDING.remove(payload.entityId());
		List<byte[]> ordered = new ArrayList<>(chunks.length);
		for (byte[] chunk : chunks) {
			ordered.add(chunk);
		}
		EchoChartCloud cloud = EchoChartCodec.decode(EchoChartCodec.join(ordered));
		READY.put(payload.entityId(), EchoChartModel.of(cloud));
	}
}
