package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.config.EchoConfig;
import dev.fimkov.echoesbeneath.config.EchoConfigValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EchoConfigRangeTest {
	@Test
	@DisplayName("the shipped defaults are all usable values")
	void defaultsAreSane() {
		EchoConfigValues values = EchoConfig.defaults();
		assertTrue(values.recordingRadius() > 0, "recording radius");
		assertTrue(values.recordingVerticalRadius() > 0, "vertical radius");
		assertTrue(values.maxRecordingSeconds() > 0, "recording seconds");
		assertTrue(values.entitySampleRate() > 0, "entity sample rate");
		assertTrue(values.maxRecordedEntities() > 0, "recorded entities");
		assertTrue(values.maxSoundEvents() > 0, "sound events");
		assertTrue(values.maxParticleEvents() >= 0, "particle events");
		assertTrue(values.maxConcurrentSoundWaves() > 0, "concurrent waves");
	}

	@Test
	@DisplayName("recording seconds convert to whole ticks")
	void secondsBecomeTicks() {
		EchoConfigValues values = EchoConfig.defaults();
		assertEquals(values.maxRecordingSeconds() * 20, values.maxRecordingTicks());
	}

	@Test
	@DisplayName("particles get a larger budget than sounds, because they arrive faster")
	void particlesGetRoomToBreathe() {
		EchoConfigValues values = EchoConfig.defaults();
		assertTrue(values.maxParticleEvents() >= values.maxSoundEvents(),
				"particles are budgeted below sounds");
	}
}
