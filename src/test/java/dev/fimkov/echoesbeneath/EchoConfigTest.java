package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.config.EchoConfig;
import dev.fimkov.echoesbeneath.config.EchoConfigValues;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoConfigTest {
	@Test
	@DisplayName("the documented defaults are the shipped defaults")
	void defaultsMatchTheDocumentation() {
		EchoConfigValues defaults = EchoConfig.defaults();
		assertEquals(50, defaults.recordingRadius());
		assertEquals(32, defaults.recordingVerticalRadius());
		assertEquals(120, defaults.maxRecordingSeconds());
		assertEquals(2400, defaults.maxRecordingTicks());
		assertEquals(5, defaults.entitySampleRate());
		assertEquals(128, defaults.maxRecordedEntities());
		assertEquals(4096, defaults.maxSoundEvents());
		assertEquals(64, defaults.maxConcurrentSoundWaves());
		assertFalse(defaults.allowOverlappingPlayback());
		assertTrue(defaults.suppressHostileMobs());
		assertTrue(defaults.disableBlockInteractionDuringPlayback());
	}

	@Test
	@DisplayName("unsafe values are clamped into a supported range")
	void unsafeValuesAreClamped() {
		Properties properties = new Properties();
		properties.setProperty("recordingRadius", "5000");
		properties.setProperty("recordingVerticalRadius", "0");
		properties.setProperty("maxRecordingSeconds", "99999");
		properties.setProperty("entitySampleRate", "-4");
		properties.setProperty("maxSoundEvents", "1");

		EchoConfigValues values = EchoConfig.sanitize(properties);
		assertTrue(values.recordingRadius() <= 64);
		assertTrue(values.recordingVerticalRadius() >= 4);
		assertTrue(values.maxRecordingSeconds() <= 120);
		assertTrue(values.entitySampleRate() >= 1);
		assertTrue(values.maxSoundEvents() >= 64);
	}

	@Test
	@DisplayName("unreadable values fall back to the defaults")
	void unreadableValuesFallBack() {
		Properties properties = new Properties();
		properties.setProperty("recordingRadius", "not-a-number");
		properties.setProperty("suppressHostileMobs", "perhaps");
		EchoConfigValues values = EchoConfig.sanitize(properties);
		assertEquals(50, values.recordingRadius());
		assertTrue(values.suppressHostileMobs());
	}

	@Test
	@DisplayName("the entity sample interval follows the configured rate")
	void sampleIntervalFollowsRate() {
		Properties properties = new Properties();
		properties.setProperty("entitySampleRate", "5");
		assertEquals(4, EchoConfig.sanitize(properties).entitySampleIntervalTicks());
		properties.setProperty("entitySampleRate", "10");
		assertEquals(2, EchoConfig.sanitize(properties).entitySampleIntervalTicks());
	}
}
