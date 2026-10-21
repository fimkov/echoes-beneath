package dev.fimkov.echoesbeneath.client.sound;

import java.io.IOException;
import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;

public final class EchoOffsetAudioStream implements AudioStream {
	private final AudioStream source;
	private long remaining;

	public EchoOffsetAudioStream(AudioStream source, int offsetTicks, float pitch) {
		this.source = source;
		AudioFormat format = source.getFormat();
		remaining = Math.round(offsetTicks / 20.0D * pitch * format.getFrameRate()) * format.getFrameSize();
	}

	@Override
	public AudioFormat getFormat() { return source.getFormat(); }

	@Override
	public ByteBuffer read(int size) throws IOException {
		while (remaining > 0) {
			ByteBuffer skipped = source.read((int) Math.min(remaining, 65536));
			if (skipped == null) { remaining = 0; return null; }
			int count = skipped.remaining();
			if (count == 0) { remaining = 0; return null; }
			if (count > remaining) {
				skipped.position(skipped.position() + (int) remaining);
				remaining = 0;
				return skipped.slice().order(skipped.order());
			}
			remaining -= count;
		}
		return source.read(size);
	}

	@Override
	public void close() throws IOException { source.close(); }
}
