package dev.fimkov.echoesbeneath.chart;

import dev.fimkov.echoesbeneath.network.EchoPayloadLimits;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class EchoChartCodec {
	private EchoChartCodec() {
	}

	public static byte[] encode(EchoChartShape shape) {
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();

				DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
			List<BlockState> states = shape.palette().states();
			out.writeInt(states.size());

			for (BlockState state : states) {
				out.writeInt(Block.getId(state));
			}
			out.writeInt(shape.lowestRelativeY());
			out.writeInt(shape.highestRelativeY());
			out.writeInt(shape.recordedVision());
			out.writeInt(shape.size());

			for (int index = 0; index < shape.size(); index++) {
				out.writeInt(shape.positions().getInt(index));
				out.writeShort(shape.states().getInt(index));
			}
			out.flush();
			out.close();

			return bytes.toByteArray();
		} catch (IOException failure) {
			throw new UncheckedIOException(failure);
		}
	}

	public static EchoChartCloud decode(byte[] encoded) {
		if (encoded.length == 0) {
			throw new IllegalArgumentException("Chart carries no bytes");
		}

		try (DataInputStream in = new DataInputStream(

				new GZIPInputStream(new ByteArrayInputStream(encoded)))) {
			int paletteSize = in.readInt();

			if (paletteSize < 0 || paletteSize > EchoPayloadLimits.MAX_CHUNKS_PER_PART * 16) {
				throw new IllegalArgumentException("Chart palette of " + paletteSize + " is out of range");
			}
			List<BlockState> palette = new ArrayList<>(paletteSize);

			for (int index = 0; index < paletteSize; index++) {
				palette.add(Block.stateById(in.readInt()));
			}
			int lowest = in.readInt();
			int highest = in.readInt();
			int vision = in.readInt();
			int count = in.readInt();

			if (count < 0 || count > EchoChartLimits.MAX_CHARTED_BLOCKS) {
				throw new IllegalArgumentException("Chart of " + count + " blocks is out of range");
			}
			IntList positions = new IntArrayList(count);
			IntList states = new IntArrayList(count);

			for (int index = 0; index < count; index++) {
				positions.add(in.readInt());
				states.add(in.readUnsignedShort());
			}
			return new EchoChartCloud(List.copyOf(palette), positions, states, lowest, highest, vision);
		} catch (IOException failure) {
			throw new UncheckedIOException(failure);
		}
	}

	public static List<byte[]> split(byte[] encoded) {
		List<byte[]> chunks = new ArrayList<>();
		int offset = 0;

		while (offset < encoded.length) {
			int length = Math.min(EchoPayloadLimits.MAX_CHUNK_BYTES, encoded.length - offset);
			byte[] chunk = new byte[length];
			System.arraycopy(encoded, offset, chunk, 0, length);
			chunks.add(chunk);
			offset += length;
		}

		if (chunks.isEmpty()) {
			chunks.add(new byte[0]);
		}

		return chunks;
	}

	public static byte[] join(List<byte[]> chunks) {
		int total = 0;

		for (byte[] chunk : chunks) {
			total += chunk.length;
		}

		byte[] joined = new byte[total];
		int offset = 0;

		for (byte[] chunk : chunks) {
			System.arraycopy(chunk, 0, joined, offset, chunk.length);
			offset += chunk.length;
		}

		return joined;
	}
}
