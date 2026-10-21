package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.client.sound.EchoOffsetAudioStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoOffsetAudioStreamTest {
	private static final AudioFormat FORMAT = new AudioFormat(1000, 16, 1, true, false);

	@Test
	void trimsExactSampleFramesAndKeepsTheRemainderOfADecoderChunk() throws IOException {
		FakeStream source = new FakeStream(1000);
		try (var stream = new EchoOffsetAudioStream(source, 2, 1.0F)) {
			ByteBuffer data = stream.read(256);
			assertEquals(312, data.remaining());
			assertEquals((byte) 200, data.get(0));
		}
		assertTrue(source.closed);
	}

	@Test
	void trimPastTheEndProducesNoAudioAndPitchChangesTheOffset() throws IOException {
		try (var stream = new EchoOffsetAudioStream(new FakeStream(100), 20, 1.0F)) {
			assertNull(stream.read(128));
		}
		try (var stream = new EchoOffsetAudioStream(new FakeStream(1000), 2, 2.0F)) {
			ByteBuffer data = stream.read(256);
			assertEquals((byte) 400, data.get(0));
		}
	}

	private static final class FakeStream implements AudioStream {
		private final int length;
		private int cursor;
		private boolean closed;
		private FakeStream(int length) { this.length = length; }
		@Override public AudioFormat getFormat() { return FORMAT; }
		@Override public ByteBuffer read(int size) {
			if (cursor >= length) { return null; }
			int count = Math.min(512, length - cursor);
			ByteBuffer data = ByteBuffer.allocateDirect(count);
			for (int i = 0; i < count; i++) { data.put((byte) cursor++); }
			return data.flip();
		}
		@Override public void close() { closed = true; }
	}
}
