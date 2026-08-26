package dev.fimkov.echoesbeneath.playback;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.memory.EchoMemoryRecord;
import dev.fimkov.echoesbeneath.network.EchoPayloadLimits;
import dev.fimkov.echoesbeneath.network.EchoPlaybackDataPart;
import dev.fimkov.echoesbeneath.network.PlaybackDataPayload;
import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EchoPlaybackTransfer {
	private EchoPlaybackTransfer() {
	}

	public static Map<EchoPlaybackDataPart, List<byte[]>> encode(EchoMemoryRecord record) {
		Map<EchoPlaybackDataPart, List<byte[]>> parts = new EnumMap<>(EchoPlaybackDataPart.class);
		parts.put(EchoPlaybackDataPart.SNAPSHOT, split(compress(record.snapshot().toNbt())));
		parts.put(EchoPlaybackDataPart.BLOCK_TIMELINE, split(compress(record.blockTimeline().toNbt())));
		parts.put(EchoPlaybackDataPart.ENTITY_TIMELINE, split(compress(record.entityTimeline().toNbt())));
		parts.put(EchoPlaybackDataPart.SOUND_TIMELINE, split(compress(record.soundTimeline().toNbt())));
		parts.put(EchoPlaybackDataPart.PARTICLE_TIMELINE,
				split(compress(record.particleTimeline().toNbt())));

		return parts;
	}

	private static byte[] compress(CompoundTag tag) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		try {
			NbtIo.writeCompressed(tag, buffer);
		} catch (IOException failure) {
			throw new IllegalStateException("Unable to encode playback data", failure);
		}

		return buffer.toByteArray();
	}

	private static List<byte[]> split(byte[] data) {
		List<byte[]> chunks = new ArrayList<>();
		int offset = 0;

		while (offset < data.length) {
			int length = Math.min(EchoPayloadLimits.MAX_CHUNK_BYTES, data.length - offset);
			byte[] chunk = new byte[length];
			System.arraycopy(data, offset, chunk, 0, length);
			chunks.add(chunk);
			offset += length;
		}

		if (chunks.isEmpty()) {
			chunks.add(new byte[0]);
		}

		if (chunks.size() > EchoPayloadLimits.MAX_CHUNKS_PER_PART) {
			throw new IllegalStateException("Playback data needs " + chunks.size() + " chunks, which exceeds the limit");
		}

		return chunks;
	}

	public static void sendTo(ServerPlayer player, UUID playbackId, Map<EchoPlaybackDataPart, List<byte[]>> parts) {
		for (Map.Entry<EchoPlaybackDataPart, List<byte[]>> entry : parts.entrySet()) {
			List<byte[]> chunks = entry.getValue();

			for (int index = 0; index < chunks.size(); index++) {
				EchoPlatforms.network().sendToPlayer(player,
						new PlaybackDataPayload(playbackId, entry.getKey(), index, chunks.size(), chunks.get(index)));
			}
		}

		EchoesBeneath.LOGGER.debug("Sent playback {} data to {}", playbackId, player.getGameProfile().getName());
	}
}
