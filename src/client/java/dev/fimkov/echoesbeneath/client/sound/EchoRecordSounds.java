package dev.fimkov.echoesbeneath.client.sound;

import dev.fimkov.echoesbeneath.network.RecordSoundPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;

public final class EchoRecordSounds {
	private static final Map<UUID, Map<Integer, SoundInstance>> PLAYING = new HashMap<>();

	private EchoRecordSounds() {
	}

	public static void accept(RecordSoundPayload payload) {
		Minecraft client = Minecraft.getInstance();
		var manager = client.getSoundManager();
		if (payload.action() == RecordSoundPayload.STOP_ALL) {
			Map<Integer, SoundInstance> sounds = PLAYING.remove(payload.playback());
			if (sounds != null) {
				sounds.values().forEach(manager::stop);
			}
			return;
		}
		Map<Integer, SoundInstance> sounds = PLAYING.computeIfAbsent(payload.playback(), ignored -> new HashMap<>());
		sounds.values().removeIf(sound -> !manager.isActive(sound));
		SoundInstance previous = sounds.remove(payload.voice());
		if (previous != null) {
			manager.stop(previous);
		}
		if (payload.action() != RecordSoundPayload.PLAY || client.level == null || payload.volume() <= 0.0F) {
			return;
		}
		SoundInstance sound = new EchoRecordSoundInstance(payload, client.level.getRandom());
		sounds.put(payload.voice(), sound);
		manager.play(sound);
	}

	public static void clear() {
		var manager = Minecraft.getInstance().getSoundManager();
		PLAYING.values().forEach(sounds -> sounds.values().forEach(manager::stop));
		PLAYING.clear();
	}
}
