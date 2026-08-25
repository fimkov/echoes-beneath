package dev.fimkov.echoesbeneath.client.sound;

import dev.fimkov.echoesbeneath.client.config.EchoClientConfig;
import dev.fimkov.echoesbeneath.client.render.EchoDarkness;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public final class EchoRegionAmbience extends AbstractTickableSoundInstance {
	private static final float FOLLOW_RATE = 0.06F;
	private static final float SILENT = 0.002F;

	private static EchoRegionAmbience current;

	private EchoRegionAmbience() {
		super(EchoSounds.PLAYBACK_AMBIENT, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
		this.looping = true;
		this.delay = 0;
		this.relative = true;
		this.attenuation = SoundInstance.Attenuation.NONE;
		this.volume = 0.0F;
		this.pitch = 1.0F;
		this.x = 0.0D;
		this.y = 0.0D;
		this.z = 0.0D;
	}

	public static void follow() {
		if (!EchoDarkness.isTransitionActive()) {
			return;
		}
		if (current != null && !current.isStopped()) {
			return;
		}
		current = new EchoRegionAmbience();
		Minecraft.getInstance().getSoundManager().play(current);
	}

	public static void forget() {
		if (current != null) {
			current.stop();
			current = null;
		}
	}

	@Override
	public void tick() {
		float target = EchoDarkness.transitionProgress() * EchoClientConfig.values().regionAmbienceGain();
		volume += (target - volume) * FOLLOW_RATE;
		if (target <= 0.0F && volume < SILENT) {
			stop();
		}
	}
}
