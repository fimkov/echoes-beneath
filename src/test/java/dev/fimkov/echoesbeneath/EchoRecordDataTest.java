package dev.fimkov.echoesbeneath;

import com.mojang.serialization.JsonOps;
import dev.fimkov.echoesbeneath.record.EchoRecordData;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EchoRecordDataTest {
	@Test
	void editsRoundTripWithTheirSourceAndVolume() {
		EchoRecordData edit = new EchoRecordData(UUID.randomUUID(), 20, 120, 65);
		var saved = EchoRecordData.CODEC.encodeStart(JsonOps.INSTANCE, edit).getOrThrow();
		assertEquals(edit, EchoRecordData.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow());
		assertEquals(100, edit.durationTicks());
		assertEquals(0.65F, edit.gain());
	}

	@Test
	void trimCannotLeaveTheRecordingOrBecomeEmpty() {
		UUID id = UUID.randomUUID();
		assertEquals(new EchoRecordData(id, 0, 200, 100), EchoRecordData.edited(id, 200, -10, 5000, 500));
		assertEquals(new EchoRecordData(id, 199, 200, 0), EchoRecordData.edited(id, 200, 500, 0, -1));
		assertThrows(IllegalArgumentException.class, () -> new EchoRecordData(id, 20, 20, 100));
		assertThrows(IllegalArgumentException.class, () -> new EchoRecordData(id, 0, 2401, 100));
	}
}
