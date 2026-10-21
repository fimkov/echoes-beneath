package dev.fimkov.echoesbeneath.client.sound;

import dev.fimkov.echoesbeneath.client.config.EchoClientConfig;
import dev.fimkov.echoesbeneath.client.wave.EchoWaveManager;
import dev.fimkov.echoesbeneath.client.wave.EchoWavePalette;
import dev.fimkov.echoesbeneath.memory.timeline.SoundTimeline;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class EchoSoundPlayer {
	private static final int[] ECHO_DELAY_TICKS = {7, 15};
	private static final float[] ECHO_VOLUME_FACTORS = {0.45F, 0.2F};
	private static final float[] ECHO_PITCH_FACTORS = {0.94F, 0.88F};
	private static final int MAX_PENDING_ECHOES = 256;
	private static final int BURST_PARTICLES = 7;
	private static final double BURST_SPEED = 0.045D;
	private static final float BURST_ROLL = 0.8F;
	private static final float SONIC_BOOM_RADIUS = 26.0F;
	private static final float LOST_SOUND_VOLUME = 0.55F;

	private final SoundTimeline timeline;
	private final EchoWaveManager waves;
	private final Vec3 center;
	private final List<PendingEcho> pendingEchoes = new ArrayList<>();
	private final Map<BlockPos, PlayingJukebox> jukeboxes = new HashMap<>();
	private final Map<BlockPos, Integer> resyncedJukeboxes = new HashMap<>();
	private int cursor;

	public EchoSoundPlayer(SoundTimeline timeline, EchoWaveManager waves, Vec3 center) {
		this.timeline = timeline;
		this.waves = waves;
		this.center = center;
	}

	public void resyncTo(int tick) {
		pendingEchoes.clear();
		resyncedJukeboxes.clear();
		cursor = 0;

		while (cursor < timeline.size() && timeline.tickAt(cursor) < tick) {
			if (timeline.isJukeboxAt(cursor)) {
				resyncedJukeboxes.put(BlockPos.containing(positionAt(cursor)), cursor);
			}
			cursor++;
		}
		var sounds = Minecraft.getInstance().getSoundManager();
		jukeboxes.entrySet().removeIf(entry -> {
			if (resyncedJukeboxes.containsKey(entry.getKey())) {
				return false;
			}
			sounds.stop(entry.getValue().sound());
			return true;
		});
	}

	public void playForTick(ClientLevel level, int tick) {
		for (int index : resyncedJukeboxes.values()) {
			playJukebox(level, index, tick);
		}
		resyncedJukeboxes.clear();
		while (cursor < timeline.size() && timeline.tickAt(cursor) <= tick) {
			emit(level, cursor, tick);
			cursor++;
		}

		playPendingEchoes(level, tick);
	}

	private void emit(ClientLevel level, int index, int tick) {
		if (timeline.isJukeboxAt(index)) {
			playJukebox(level, index, tick);
			return;
		}
		ResourceLocation soundId = timeline.soundAt(index);
		SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(soundId);
		boolean remembered = event != null;

		if (!remembered) {
			event = EchoSounds.WAVE_ECHO;
		}

		Vec3 position = center.add(
				timeline.relativeXAt(index), timeline.relativeYAt(index), timeline.relativeZAt(index));
		float volume = timeline.volumeAt(index)
				* EchoClientConfig.values().waveGain();
		float pitch = timeline.pitchAt(index);
		SoundSource source = timeline.categoryAt(index);
		level.playLocalSound(position.x, position.y, position.z, event, source,
				remembered ? volume : volume * LOST_SOUND_VOLUME, pitch, false);
		float radius = timeline.waveRadiusAt(index);
		waves.spawn(position, tick, radius,
				EchoWavePalette.paletteFor(source));
		burst(level, position, radius);

		if (remembered) {
			scheduleEchoes(tick, event, source, position, volume, pitch);
		}
	}

	private void playJukebox(ClientLevel level, int index, int tick) {
		Vec3 position = positionAt(index);
		BlockPos key = BlockPos.containing(position);
		var sounds = Minecraft.getInstance().getSoundManager();
		PlayingJukebox previous = jukeboxes.get(key);
		if (previous != null && previous.index() == index) {
			return;
		}
		if (previous != null) {
			jukeboxes.remove(key);
			sounds.stop(previous.sound());
		}
		if (timeline.isStopAt(index)) {
			return;
		}
		SoundEvent event = SoundEvent.createVariableRangeEvent(timeline.soundAt(index));
		SoundInstance sound = new SimpleSoundInstance(event, SoundSource.RECORDS,
				timeline.volumeAt(index) * EchoClientConfig.values().waveGain(), timeline.pitchAt(index),
				level.getRandom(), position.x, position.y, position.z);
		jukeboxes.put(key, new PlayingJukebox(index, sound));
		sounds.play(sound);
		waves.spawn(position, tick, timeline.waveRadiusAt(index), EchoWavePalette.paletteFor(SoundSource.RECORDS));
		burst(level, position, timeline.waveRadiusAt(index));
	}

	private Vec3 positionAt(int index) {
		return center.add(timeline.relativeXAt(index), timeline.relativeYAt(index), timeline.relativeZAt(index));
	}

	private record PlayingJukebox(int index, SoundInstance sound) {
	}

	public void discard() {
		var sounds = Minecraft.getInstance().getSoundManager();
		jukeboxes.values().forEach(playing -> sounds.stop(playing.sound()));
		jukeboxes.clear();
		resyncedJukeboxes.clear();
		pendingEchoes.clear();
	}

	private void burst(ClientLevel level, Vec3 position, float radius) {
		for (int index = 0; index < BURST_PARTICLES; index++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
			double lift = (level.getRandom().nextDouble() - 0.5D) * BURST_SPEED;
			level.addParticle(
					new SculkChargeParticleOptions(
							level.getRandom().nextFloat() * BURST_ROLL),
					position.x, position.y, position.z,
					Math.cos(angle) * BURST_SPEED, lift, Math.sin(angle) * BURST_SPEED);
		}

		level.addParticle(ParticleTypes.SCULK_SOUL,
				position.x, position.y, position.z, 0.0D, 0.0D, 0.0D);

		if (radius >= SONIC_BOOM_RADIUS) {
			level.addParticle(ParticleTypes.SONIC_BOOM,
					position.x, position.y, position.z, 0.0D, 0.0D, 0.0D);
		}
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
