package dev.fimkov.echoesbeneath.client.sound;

import dev.fimkov.echoesbeneath.client.wave.EchoWaveManager;
import dev.fimkov.echoesbeneath.memory.timeline.SoundTimeline;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class EchoSoundPlayer {
	private static final int[] ECHO_DELAY_TICKS = {7, 15};
	private static final float[] ECHO_VOLUME_FACTORS = {0.45F, 0.2F};
	private static final float[] ECHO_PITCH_FACTORS = {0.94F, 0.88F};
	private static final int MAX_PENDING_ECHOES = 256;

	private final SoundTimeline timeline;
	private final EchoWaveManager waves;
	private final Vec3 center;
	private final List<PendingEcho> pendingEchoes = new ArrayList<>();
	private int cursor;

	public EchoSoundPlayer(SoundTimeline timeline, EchoWaveManager waves, Vec3 center) {
		this.timeline = timeline;
		this.waves = waves;
		this.center = center;
	}

	public void resyncTo(int tick) {
		pendingEchoes.clear();
		cursor = 0;
		while (cursor < timeline.size() && timeline.tickAt(cursor) < tick) {
			cursor++;
		}
	}

	public void playForTick(ClientLevel level, int tick) {
		while (cursor < timeline.size() && timeline.tickAt(cursor) <= tick) {
			emit(level, cursor, tick);
			cursor++;
		}
		playPendingEchoes(level, tick);
	}

	private void emit(ClientLevel level, int index, int tick) {
		ResourceLocation soundId = timeline.soundAt(index);
		SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(soundId);
		if (event == null) {
			return;
		}
		Vec3 position = center.add(
				timeline.relativeXAt(index), timeline.relativeYAt(index), timeline.relativeZAt(index));
		float volume = timeline.volumeAt(index);
		float pitch = timeline.pitchAt(index);
		SoundSource source = timeline.categoryAt(index);
		level.playLocalSound(position.x, position.y, position.z, event, source, volume, pitch, false);
		waves.spawn(position, tick, timeline.waveRadiusAt(index),
				dev.fimkov.echoesbeneath.client.wave.EchoWavePalette.paletteFor(source));
		scheduleEchoes(tick, event, source, position, volume, pitch);
	}

	private void scheduleEchoes(int tick, SoundEvent event, SoundSource source, Vec3 position,
			float volume, float pitch) {
		if (pendingEchoes.size() + ECHO_DELAY_TICKS.length > MAX_PENDING_ECHOES) {
			return;
		}
		for (int step = 0; step < ECHO_DELAY_TICKS.length; step++) {
			pendingEchoes.add(new PendingEcho(tick + ECHO_DELAY_TICKS[step], event, source, position,
					volume * ECHO_VOLUME_FACTORS[step], pitch * ECHO_PITCH_FACTORS[step]));
		}
	}

	private void playPendingEchoes(ClientLevel level, int tick) {
		pendingEchoes.removeIf(echo -> {
			if (echo.tick > tick) {
				return false;
			}
			level.playLocalSound(echo.position.x, echo.position.y, echo.position.z,
					echo.event, echo.source, echo.volume, echo.pitch, false);
			return true;
		});
	}

	private record PendingEcho(int tick, SoundEvent event, SoundSource source, Vec3 position,
			float volume, float pitch) {
	}
}
