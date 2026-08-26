package dev.fimkov.echoesbeneath.chart;

import dev.fimkov.echoesbeneath.entity.EchoChartEntity;
import dev.fimkov.echoesbeneath.memory.EchoMemoryRecord;
import dev.fimkov.echoesbeneath.memory.storage.EchoMemoryStorage;
import dev.fimkov.echoesbeneath.network.ChartShapePayload;
import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EchoChartService {
	private final EchoMemoryStorage storage;
	private final Map<String, List<byte[]>> encoded = new HashMap<>();

	public EchoChartService(EchoMemoryStorage storage) {
		this.storage = storage;
	}

	public void forget(UUID memoryId) {
		encoded.keySet().removeIf(key -> key.startsWith(memoryId.toString()));
	}

	public void sendTo(Entity tracked, ServerPlayer viewer) {
		if (!(tracked instanceof EchoChartEntity chart)) {
			return;
		}
		EchoChartData data = chart.chartData();
		if (data == null) {
			return;
		}
		List<byte[]> chunks = chunksFor(data);
		if (chunks == null) {
			return;
		}
		for (int index = 0; index < chunks.size(); index++) {
			EchoPlatforms.network().sendToPlayer(viewer,
					new ChartShapePayload(chart.getId(), index, chunks.size(), chunks.get(index)));
		}
	}

	private List<byte[]> chunksFor(EchoChartData data) {
		String key = cacheKey(data);
		List<byte[]> cached = encoded.get(key);
		if (cached != null) {
			return cached;
		}
		Optional<EchoMemoryRecord> record = storage.load(data.memoryId());
		if (record.isEmpty()) {
			return null;
		}
		EchoChartShape shape = EchoSounding.of(record.get(), data.lowestY(), data.highestY(),
				EchoChartLimits.MAX_CHARTED_BLOCKS);
		List<byte[]> chunks = EchoChartCodec.split(EchoChartCodec.encode(shape));
		encoded.put(key, chunks);
		return chunks;
	}

	private static String cacheKey(EchoChartData data) {
		return data.memoryId() + ":" + data.lowestY() + ":" + data.highestY();
	}
}
