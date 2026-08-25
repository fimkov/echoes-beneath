package dev.fimkov.echoesbeneath.client.wave;

import dev.fimkov.echoesbeneath.memory.timeline.SoundWaveStrength;
import net.minecraft.world.phys.Vec3;

public final class EchoWave {
	private static final float SHELL_THICKNESS = 3.2F;

	private final Vec3 origin;
	private final int startTick;
	private final float maxRadius;

	public EchoWave(Vec3 origin, int startTick, float maxRadius) {
		this.origin = origin;
		this.startTick = startTick;
		this.maxRadius = maxRadius;
	}

	public Vec3 origin() {
		return origin;
	}

	public float maxRadius() {
		return maxRadius;
	}

	public float radiusAt(float elapsedTicks) {
		return Math.max(0.0F, elapsedTicks) * SoundWaveStrength.WAVE_SPEED_BLOCKS_PER_TICK;
	}

	public float elapsedAt(int currentTick, float partialTick) {
		return currentTick - startTick + partialTick;
	}

	public boolean isAlive(int currentTick) {
		return radiusAt(currentTick - startTick) <= maxRadius;
	}

	public float shellThickness() {
		return SHELL_THICKNESS;
	}

	public float fadeAt(float elapsedTicks) {
		float radius = radiusAt(elapsedTicks);
		if (radius >= maxRadius) {
			return 0.0F;
		}
		return 1.0F - radius / maxRadius;
	}

	public float depthOf(double distance, float elapsedTicks) {
		float front = radiusAt(elapsedTicks);
		float delta = (float) (front - distance);
		if (delta < 0.0F || delta > SHELL_THICKNESS) {
			return -1.0F;
		}
		return delta / SHELL_THICKNESS;
	}
}
