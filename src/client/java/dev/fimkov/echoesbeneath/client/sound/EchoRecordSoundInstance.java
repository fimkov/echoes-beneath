package dev.fimkov.echoesbeneath.client.sound;

import dev.fimkov.echoesbeneath.network.RecordSoundPayload;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class EchoRecordSoundInstance extends SimpleSoundInstance {
	private final int offsetTicks;

	public EchoRecordSoundInstance(RecordSoundPayload payload, RandomSource random) {
		super(SoundEvent.createVariableRangeEvent(payload.sound()), SoundSource.RECORDS, payload.volume(),
				payload.pitch(), random, payload.speaker().getX() + 0.5D,
				payload.speaker().getY() + 0.5D, payload.speaker().getZ() + 0.5D);
		this.offsetTicks = payload.offsetTicks();
	}

	public int offsetTicks() { return offsetTicks; }

	@Override
	public WeighedSoundEvents resolve(SoundManager manager) {
		WeighedSoundEvents events = super.resolve(manager);
		if (sound != null && sound != SoundManager.EMPTY_SOUND) {
			sound = new Sound(sound.getLocation(), sound.getVolume(), sound.getPitch(), sound.getWeight(),
					sound.getType(), offsetTicks > 0 || sound.shouldStream(), false, 64);
		}
		return events;
	}
}
