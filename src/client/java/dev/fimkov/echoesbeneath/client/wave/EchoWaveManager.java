package dev.fimkov.echoesbeneath.client.wave;

import dev.fimkov.echoesbeneath.config.EchoConfig;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class EchoWaveManager {
	private final Deque<EchoWave> waves = new ArrayDeque<>();

	public void spawn(Vec3 origin, int startTick, float maxRadius, int paletteIndex) {
		while (waves.size() >= EchoConfig.values().maxConcurrentSoundWaves()) {
			waves.pollFirst();
		}

		waves.addLast(new EchoWave(origin, startTick, maxRadius, paletteIndex));
	}

	public void prune(int currentTick) {
		waves.removeIf(wave -> !wave.isAlive(currentTick));
	}

	public List<EchoWave> active() {
		return List.copyOf(waves);
	}

	public int size() {
		return waves.size();
	}

	public void clear() {
		waves.clear();
	}
}
