package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.memory.timeline.ParticleTimeline;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoParticleTimelineTest {
	private static final int MAX_EVENTS = 512;
	private static final ResourceLocation SOUL = ResourceLocation.withDefaultNamespace("sculk_soul");
	private static final ResourceLocation SMOKE = ResourceLocation.withDefaultNamespace("smoke");

	@Test
	@DisplayName("a recorded burst keeps its tick, place, type, count and spread")
	void burstsSurviveARoundTrip() {
		ParticleTimeline timeline = new ParticleTimeline(MAX_EVENTS);
		timeline.record(41, 3.5D, -2.25D, 7.75D, SOUL, 5, 0.3D, 0.15D, 0.3D);
		ParticleTimeline restored = ParticleTimeline.fromNbt(timeline.toNbt(), MAX_EVENTS);
		assertEquals(1, restored.size());
		assertEquals(41, restored.tickAt(0));
		assertEquals(SOUL, restored.typeAt(0));
		assertEquals(5, restored.countAt(0));
		assertEquals(3.5D, restored.relativeXAt(0), 0.05D);
		assertEquals(-2.25D, restored.relativeYAt(0), 0.05D);
		assertEquals(7.75D, restored.relativeZAt(0), 0.05D);
		assertEquals(0.3D, restored.spreadXAt(0), 0.005D);
	}

	@Test
	@DisplayName("the same burst repeated in one tick is only kept once")
	void repeatsCollapse() {
		ParticleTimeline timeline = new ParticleTimeline(MAX_EVENTS);
		for (int repeat = 0; repeat < 20; repeat++) {
			timeline.record(9, 1.0D, 1.0D, 1.0D, SMOKE, 1, 0.0D, 0.0D, 0.0D);
		}
		assertEquals(1, timeline.size());
	}

	@Test
	@DisplayName("different types in the same place are both kept")
	void differentTypesAreKept() {
		ParticleTimeline timeline = new ParticleTimeline(MAX_EVENTS);
		timeline.record(9, 1.0D, 1.0D, 1.0D, SMOKE, 1, 0.0D, 0.0D, 0.0D);
		timeline.record(9, 1.0D, 1.0D, 1.0D, SOUL, 1, 0.0D, 0.0D, 0.0D);
		assertEquals(2, timeline.size());
	}

	@Test
	@DisplayName("the timeline stops at its ceiling instead of growing without bound")
	void theCeilingHolds() {
		ParticleTimeline timeline = new ParticleTimeline(4);
		for (int tick = 0; tick < 40; tick++) {
			timeline.record(tick, 0.0D, 0.0D, 0.0D, SOUL, 1, 0.0D, 0.0D, 0.0D);
		}
		assertEquals(4, timeline.size());
	}

	@Test
	@DisplayName("a memory that recorded no particles still reads back")
	void emptyTimelineRoundTrips() {
		ParticleTimeline restored = ParticleTimeline.fromNbt(new ParticleTimeline(MAX_EVENTS).toNbt(), MAX_EVENTS);
		assertEquals(0, restored.size());
		assertTrue(restored.typePalette().isEmpty());
	}
}
