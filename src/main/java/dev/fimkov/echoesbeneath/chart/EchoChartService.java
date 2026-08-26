package dev.fimkov.echoesbeneath.chart;

import dev.fimkov.echoesbeneath.blockentity.EchoChartBlockEntity;
import dev.fimkov.echoesbeneath.memory.EchoMemoryRecord;
import dev.fimkov.echoesbeneath.memory.storage.EchoMemoryStorage;
import dev.fimkov.echoesbeneath.network.ChartShapePayload;
import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EchoChartService {
	private final EchoMemoryStorage storage;
	private static final int MAX_REMEMBERED_FLOODS = 4;
	private static final double REACH = 96.0D;

	private final Map<String, List<byte[]>> encoded = new HashMap<>();
	private final Map<UUID, EchoSounding.Columns> floods = new LinkedHashMap<>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<UUID, EchoSounding.Columns> eldest) {
			return size() > MAX_REMEMBERED_FLOODS;
		}
	};

	public EchoChartService(EchoMemoryStorage storage) {
		this.storage = storage;
	}

	public void forget(UUID memoryId) {
		encoded.keySet().removeIf(key -> key.startsWith(memoryId.toString()));
		floods.remove(memoryId);
	}

	public EchoChartShape shapeOf(EchoMemoryRecord record, int lowestY, int highestY) {
		EchoSounding.Columns reach = floods.get(record.id());
		if (reach == null) {
			reach = EchoSounding.reach(record);
			floods.put(record.id(), reach);
		}
		return EchoSounding.of(record, reach, lowestY, highestY, EchoChartLimits.MAX_CHARTED_BLOCKS);
	}

	public void answer(ServerPlayer viewer, BlockPos position) {
		if (!viewer.level().isLoaded(position)
				|| viewer.distanceToSqr(position.getX() + 0.5D, position.getY() + 0.5D,
						position.getZ() + 0.5D) > REACH * REACH) {
			return;
		}
		if (!(viewer.level().getBlockEntity(position) instanceof EchoChartBlockEntity chart)) {
			return;
		}
		EchoChartData data = chart.chart();
		if (data == null) {
			return;
		}
		List<byte[]> chunks = chunksFor(data);
		if (chunks == null) {
			return;
		}
		for (int index = 0; index < chunks.size(); index++) {
			EchoPlatforms.network().sendToPlayer(viewer,
					new ChartShapePayload(position.asLong(), index, chunks.size(), chunks.get(index)));
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
		EchoChartShape shape = shapeOf(record.get(), data.lowestY(), data.highestY());
		List<byte[]> chunks = EchoChartCodec.split(EchoChartCodec.encode(shape));
		encoded.put(key, chunks);
		return chunks;
	}

	private static String cacheKey(EchoChartData data) {
		return data.memoryId() + ":" + data.lowestY() + ":" + data.highestY();
	}
}
