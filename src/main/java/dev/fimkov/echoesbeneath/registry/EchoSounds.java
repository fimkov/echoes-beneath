package dev.fimkov.echoesbeneath.registry;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class EchoSounds {
	public static final SoundEvent LENS_DEPLOY = SoundEvents.AMETHYST_BLOCK_RESONATE;
	public static final SoundEvent LENS_RECORDING_START = SoundEvents.SCULK_CLICKING;
	public static final SoundEvent LENS_RECORDING_COMPLETE = SoundEvents.AMETHYST_BLOCK_CHIME;
	public static final SoundEvent LENS_RETURN = SoundEvents.AMETHYST_CLUSTER_BREAK;
	public static final SoundEvent PLAYBACK_START = SoundEvents.SCULK_SHRIEKER_SHRIEK;
	public static final SoundEvent PLAYBACK_END = SoundEvents.SCULK_CATALYST_BLOOM;
	public static final SoundEvent RESONANCE_PING = SoundEvents.AMETHYST_BLOCK_HIT;
	public static final SoundEvent WAVE_ECHO = SoundEvents.SCULK_BLOCK_CHARGE;

	private EchoSounds() {
	}

	public static void register() {
	}
}
