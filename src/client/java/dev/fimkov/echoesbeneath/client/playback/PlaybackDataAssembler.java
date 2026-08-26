package dev.fimkov.echoesbeneath.client.playback;

import dev.fimkov.echoesbeneath.network.EchoPlaybackDataPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public final class PlaybackDataAssembler {
	private static final long MAX_DECODED_BYTES = 64L * 1024L * 1024L;

	private final Map<EchoPlaybackDataPart, byte[][]> chunks = new EnumMap<>(EchoPlaybackDataPart.class);
	private final Map<EchoPlaybackDataPart, Integer> received = new EnumMap<>(EchoPlaybackDataPart.class);

	public void accept(EchoPlaybackDataPart part, int chunkIndex, int chunkCount, byte[] chunk) {
		byte[][] slots = chunks.computeIfAbsent(part, ignored -> new byte[chunkCount][]);

		if (slots.length != chunkCount || chunkIndex >= slots.length) {
			throw new IllegalStateException("Playback data chunk count changed mid transfer");
		}

		if (slots[chunkIndex] == null) {
			received.merge(part, 1, Integer::sum);
		}

		slots[chunkIndex] = chunk;
	}

	public boolean isComplete() {
		for (EchoPlaybackDataPart part : EchoPlaybackDataPart.values()) {
			byte[][] slots = chunks.get(part);

			if (slots == null || received.getOrDefault(part, 0) != slots.length) {
				return false;
			}
		}

		return true;
	}

	public CompoundTag decode(EchoPlaybackDataPart part) throws IOException {
		byte[][] slots = chunks.get(part);
		int totalLength = 0;

		for (byte[] slot : slots) {
			totalLength += slot.length;
		}

		byte[] joined = new byte[totalLength];
		int offset = 0;

		for (byte[] slot : slots) {
			System.arraycopy(slot, 0, joined, offset, slot.length);
			offset += slot.length;
		}

		return NbtIo.readCompressed(new ByteArrayInputStream(joined), NbtAccounter.create(MAX_DECODED_BYTES));
	}
}
