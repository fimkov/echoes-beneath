package dev.fimkov.echoesbeneath.chart;

import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import dev.fimkov.echoesbeneath.memory.EchoMemoryRecord;
import dev.fimkov.echoesbeneath.network.ChartShapePayload;
import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import dev.fimkov.echoesbeneath.runtime.EchoServerState;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public final class EchoChartPreview {
	public static final long MENU_TARGET = ChartShapePayload.PREVIEW_TARGET;

	private EchoChartPreview() {
	}

	public static void send(ServerPlayer viewer, EchoMemoryReference reference, int lowestY, int highestY) {
		EchoChartShape shape = shapeOf(viewer, reference, lowestY, highestY);
		if (shape == null) {
			EchoPlatforms.network().sendToPlayer(viewer,
					new ChartShapePayload(MENU_TARGET, 0, 1, new byte[0]));
			return;
		}
		List<byte[]> chunks = EchoChartCodec.split(EchoChartCodec.encode(shape));
		for (int index = 0; index < chunks.size(); index++) {
			EchoPlatforms.network().sendToPlayer(viewer,
					new ChartShapePayload(MENU_TARGET, index, chunks.size(), chunks.get(index)));
		}
	}

	public static int soundedCount(ServerPlayer viewer, EchoMemoryReference reference,
			int lowestY, int highestY) {
		EchoChartShape shape = shapeOf(viewer, reference, lowestY, highestY);
		return shape == null ? 0 : shape.size();
	}

	private static EchoChartShape shapeOf(ServerPlayer viewer, EchoMemoryReference reference,
			int lowestY, int highestY) {
		if (reference == null) {
			return null;
		}
		EchoServerState state = EchoServerState.current();
		if (state == null) {
			return null;
		}
		Optional<EchoMemoryRecord> record = state.storage().load(reference.memoryId());
		return record.map(held -> state.charts().shapeOf(held, lowestY, highestY)).orElse(null);
	}
}
