package dev.fimkov.echoesbeneath.registry;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class EchoSounds {
	public static final SoundEvent LENS_RECORDING_START = of("recording_start");
	public static final SoundEvent LENS_RECORDING_PULSE = of("recording_pulse");
	public static final SoundEvent LENS_RECORDING_COMPLETE = of("memory_seal");
	public static final SoundEvent LENS_RETURN = of("lens_return");
	public static final SoundEvent PLAYBACK_START = of("playback_start");
	public static final SoundEvent PLAYBACK_END = of("playback_end");
	public static final SoundEvent PLAYBACK_AMBIENT = of("region_ambient");
	public static final SoundEvent RESONANCE_PING = of("resonance_ping");
	public static final SoundEvent WAVE_ECHO = of("wave_pulse");
	public static final SoundEvent TABLE_TUNE = of("table_tune");
	public static final SoundEvent TABLE_DENY = of("table_deny");

	private EchoSounds() {
	}

	private static SoundEvent of(String path) {
		ResourceLocation id = EchoesBeneath.id(path);

		return SoundEvent.createVariableRangeEvent(id);
	}

	public static void register() {
		put("recording_start", LENS_RECORDING_START);
		put("recording_pulse", LENS_RECORDING_PULSE);
		put("memory_seal", LENS_RECORDING_COMPLETE);
		put("lens_return", LENS_RETURN);
		put("playback_start", PLAYBACK_START);
		put("playback_end", PLAYBACK_END);
		put("region_ambient", PLAYBACK_AMBIENT);
		put("resonance_ping", RESONANCE_PING);
		put("wave_pulse", WAVE_ECHO);
		put("table_tune", TABLE_TUNE);
		put("table_deny", TABLE_DENY);
	}

	private static void put(String path, SoundEvent event) {
		Registry.register(BuiltInRegistries.SOUND_EVENT, EchoesBeneath.id(path), event);
	}
}
