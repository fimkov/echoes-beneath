package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.memory.timeline.EchoSoundCategory;
import dev.fimkov.echoesbeneath.memory.timeline.SoundWaveStrength;
import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoSoundWaveTest {
	@Test
	@DisplayName("background music and interface sounds are never recorded")
	void nonSpatialCategoriesAreSkipped() {
		assertFalse(EchoSoundCategory.isRecordable(SoundSource.MUSIC));
		assertFalse(EchoSoundCategory.isRecordable(SoundSource.MASTER));
		assertFalse(EchoSoundCategory.isRecordable(SoundSource.VOICE));
		assertFalse(EchoSoundCategory.isRecordable(SoundSource.WEATHER));
	}

	@Test
	@DisplayName("world sounds are recorded")
	void spatialCategoriesAreRecorded() {
		assertTrue(EchoSoundCategory.isRecordable(SoundSource.RECORDS));
		assertTrue(EchoSoundCategory.isRecordable(SoundSource.BLOCKS));
		assertTrue(EchoSoundCategory.isRecordable(SoundSource.HOSTILE));
		assertTrue(EchoSoundCategory.isRecordable(SoundSource.PLAYERS));
		assertTrue(EchoSoundCategory.isRecordable(SoundSource.NEUTRAL));
	}

	@Test
	@DisplayName("quiet sounds stay small and loud sounds reach far")
	void waveRadiusScalesWithVolume() {
		float footstep = SoundWaveStrength.radiusFor(SoundSource.BLOCKS, 0.15F);
		float door = SoundWaveStrength.radiusFor(SoundSource.BLOCKS, 1.0F);
		float explosion = SoundWaveStrength.radiusFor(SoundSource.BLOCKS, 4.0F);

		assertTrue(footstep >= 3.0F && footstep <= 6.0F, "footstep radius was " + footstep);
		assertTrue(door >= 6.0F && door <= 12.0F, "door radius was " + door);
		assertTrue(explosion >= 30.0F && explosion <= 50.0F, "explosion radius was " + explosion);
	}

	@Test
	@DisplayName("no wave outgrows the recorded region")
	void waveRadiusIsCapped() {
		float enormous = SoundWaveStrength.radiusFor(SoundSource.HOSTILE, 1000.0F);
		assertTrue(enormous <= 50.0F, "radius was " + enormous);
	}

	@Test
	@DisplayName("waves expand over time rather than appearing at once")
	void wavesTakeTimeToExpand() {
		float radius = SoundWaveStrength.radiusFor(SoundSource.HOSTILE, 1.0F);
		int lifetime = SoundWaveStrength.lifetimeTicks(radius);
		assertTrue(lifetime > 1, "wave lifetime was " + lifetime);
		assertTrue(radius / lifetime <= SoundWaveStrength.WAVE_SPEED_BLOCKS_PER_TICK + 0.01F);
	}
}
