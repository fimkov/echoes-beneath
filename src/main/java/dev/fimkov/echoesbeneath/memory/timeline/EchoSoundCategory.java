package dev.fimkov.echoesbeneath.memory.timeline;

import net.minecraft.sounds.SoundSource;

public final class EchoSoundCategory {
	private static final SoundSource[] VALUES = SoundSource.values();

	private EchoSoundCategory() {
	}

	public static int indexOf(SoundSource source) {
		return source.ordinal();
	}

	public static SoundSource fromIndex(int index) {
		if (index < 0 || index >= VALUES.length) {
			return SoundSource.AMBIENT;
		}

		return VALUES[index];
	}

	public static boolean isRecordable(SoundSource source) {
		return source != SoundSource.MUSIC && source != SoundSource.RECORDS
				&& source != SoundSource.MASTER && source != SoundSource.VOICE
				&& source != SoundSource.WEATHER;
	}
}
