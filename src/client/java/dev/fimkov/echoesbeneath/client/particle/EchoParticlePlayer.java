package dev.fimkov.echoesbeneath.client.particle;

import dev.fimkov.echoesbeneath.memory.timeline.ParticleTimeline;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class EchoParticlePlayer {
	private static final int MAX_PER_EVENT = 24;
	private static final double DRIFT = 0.0D;

	private final ParticleTimeline timeline;
	private final Vec3 center;
	private int cursor;

	public EchoParticlePlayer(ParticleTimeline timeline, Vec3 center) {
		this.timeline = timeline;
		this.center = center;
	}

	public void resyncTo(int tick) {
		cursor = 0;

		while (cursor < timeline.size() && timeline.tickAt(cursor) < tick) {
			cursor++;
		}
	}

	public void playForTick(ClientLevel level, int tick) {
		while (cursor < timeline.size() && timeline.tickAt(cursor) <= tick) {
			emit(level, cursor);
			cursor++;
		}
	}

	private void emit(ClientLevel level, int index) {
		ParticleOptions options = optionsFor(timeline.typeAt(index));

		if (options == null) {
			return;
		}

		Vec3 origin = center.add(timeline.relativeXAt(index), timeline.relativeYAt(index),
				timeline.relativeZAt(index));
		int count = Math.min(MAX_PER_EVENT, timeline.countAt(index));
		double spreadX = timeline.spreadXAt(index);
		double spreadY = timeline.spreadYAt(index);
		double spreadZ = timeline.spreadZAt(index);

		for (int emitted = 0; emitted < count; emitted++) {
			level.addParticle(options,
					origin.x + level.getRandom().nextGaussian() * spreadX,
					origin.y + level.getRandom().nextGaussian() * spreadY,
					origin.z + level.getRandom().nextGaussian() * spreadZ,
					DRIFT, DRIFT, DRIFT);
		}
	}

	private static ParticleOptions optionsFor(Identifier typeId) {
		ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(typeId).map(net.minecraft.core.Holder::value).orElse(null);

		return type instanceof SimpleParticleType simple ? simple : null;
	}
}
