package dev.fimkov.echoesbeneath.client.wave;

import dev.fimkov.echoesbeneath.memory.timeline.SoundWaveStrength;
import net.minecraft.world.phys.Vec3;

public final class EchoWave {
	private static final float SHELL_THICKNESS = 4.5F;
	private static final float LOCAL_REVEAL_RADIUS = 7.5F;
	private static final float LOCAL_PULSE_TICKS = 22.0F;
	private static final float LOCAL_PULSE_DEPTH = 0.22F;

	private final Vec3 origin;
	private final int startTick;
	private final float maxRadius;
	private final int paletteIndex;

	public EchoWave(Vec3 origin, int startTick, float maxRadius, int paletteIndex) {
		this.origin = origin;
		this.startTick = startTick;
		this.maxRadius = maxRadius;
		this.paletteIndex = paletteIndex;
	}

	public int paletteIndex() {
		return paletteIndex;
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

	public float localRevealRadius() {
		return Math.min(maxRadius, LOCAL_REVEAL_RADIUS);
	}

	public float proximityAt(double distance, float elapsedTicks) {
		float reach = Math.min(maxRadius, LOCAL_REVEAL_RADIUS);
		if (distance >= reach) {
			return 0.0F;
		}
		float falloff = 1.0F - (float) (distance / reach);
		float pulse = 1.0F - LOCAL_PULSE_DEPTH
				+ LOCAL_PULSE_DEPTH * (float) Math.sin(elapsedTicks / LOCAL_PULSE_TICKS * Math.PI * 2.0D);
		return falloff * falloff * pulse;
	}

	public float fadeAt(float elapsedTicks) {
		float radius = radiusAt(elapsedTicks);
		if (radius >= maxRadius) {
			return 0.0F;
		}
		return (float) Math.sqrt(1.0F - radius / maxRadius);
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
