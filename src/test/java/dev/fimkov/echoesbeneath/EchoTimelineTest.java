package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.memory.timeline.BlockTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.EchoEntityAction;
import dev.fimkov.echoesbeneath.memory.timeline.EntityTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.EntityTrack;
import dev.fimkov.echoesbeneath.memory.timeline.SoundTimeline;
import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoTimelineTest {
	private static final int MAX_EVENTS = 4096;
	private static final Identifier DOOR_OPEN =
			Identifier.withDefaultNamespace("block.wooden_door.open");
	private static final Identifier ZOMBIE_AMBIENT =
			Identifier.withDefaultNamespace("entity.zombie.ambient");

	@Test
	@DisplayName("a sound event keeps its identifier, tick, position, volume and pitch")
	void soundEventsKeepEverythingNeededForPlayback() {
		SoundTimeline timeline = new SoundTimeline(MAX_EVENTS);
		timeline.record(120, 4.0D, 1.0D, -8.0D, ZOMBIE_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.9F);

		assertEquals(1, timeline.size());
		assertEquals(120, timeline.tickAt(0));
		assertEquals(ZOMBIE_AMBIENT, timeline.soundAt(0));
		assertEquals(4.0D, timeline.relativeXAt(0), 0.01D);
		assertEquals(1.0D, timeline.relativeYAt(0), 0.01D);
		assertEquals(-8.0D, timeline.relativeZAt(0), 0.01D);
		assertEquals(1.0F, timeline.volumeAt(0), 0.001F);
		assertEquals(0.9F, timeline.pitchAt(0), 0.001F);
		assertEquals(SoundSource.HOSTILE, timeline.categoryAt(0));
	}

	@Test
	@DisplayName("a recorded sound produces a visual wave radius")
	void soundEventsCarryAWaveRadius() {
		SoundTimeline timeline = new SoundTimeline(MAX_EVENTS);
		timeline.record(10, 0.0D, 0.0D, 0.0D, DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
		float radius = timeline.waveRadiusAt(0);
		assertTrue(radius >= 6.0F && radius <= 12.0F, "door wave radius was " + radius);
	}

	@Test
	@DisplayName("a sound timeline survives a save and load cycle")
	void soundTimelineRoundTrips() {
		SoundTimeline timeline = new SoundTimeline(MAX_EVENTS);
		timeline.record(5, 1.0D, 2.0D, 3.0D, DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
		timeline.record(64, -7.5D, 0.5D, 12.25D, ZOMBIE_AMBIENT, SoundSource.HOSTILE, 0.8F, 1.1F);

		CompoundTag saved = timeline.toNbt();
		SoundTimeline restored = SoundTimeline.fromNbt(saved, MAX_EVENTS);

		assertEquals(timeline.size(), restored.size());

		for (int index = 0; index < timeline.size(); index++) {
			assertEquals(timeline.tickAt(index), restored.tickAt(index));
			assertEquals(timeline.soundAt(index), restored.soundAt(index));
			assertEquals(timeline.relativeXAt(index), restored.relativeXAt(index), 0.01D);
			assertEquals(timeline.relativeZAt(index), restored.relativeZAt(index), 0.01D);
			assertEquals(timeline.volumeAt(index), restored.volumeAt(index), 0.001F);
			assertEquals(timeline.categoryAt(index), restored.categoryAt(index));
		}
	}

	@Test
	@DisplayName("identical sounds in the same tick and place collapse into one event")
	void duplicateSoundsCollapse() {
		SoundTimeline timeline = new SoundTimeline(MAX_EVENTS);
		timeline.record(40, 0.0D, 0.0D, 0.0D, DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
		timeline.record(40, 0.0D, 0.0D, 0.0D, DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
		assertEquals(1, timeline.size());
	}

	@Test
	@DisplayName("the sound event limit is respected")
	void soundEventLimitIsEnforced() {
		SoundTimeline timeline = new SoundTimeline(4);

		for (int tick = 0; tick < 40; tick++) {
			timeline.record(tick, tick, 0.0D, 0.0D, DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
		}

		assertEquals(4, timeline.size());
	}

	@Test
	@DisplayName("block changes keep the tick, position and both states")
	void blockTimelineKeepsBothStates() {
		BlockTimeline timeline = new BlockTimeline(MAX_EVENTS);
		int packed = EchoCoordinates.packRelative(3, -2, 7);
		timeline.record(88, packed, 4, 9);

		assertEquals(1, timeline.size());
		assertEquals(88, timeline.tickAt(0));
		assertEquals(packed, timeline.packedPositionAt(0));
		assertEquals(4, timeline.previousStateAt(0));
		assertEquals(9, timeline.newStateAt(0));

		BlockTimeline restored = BlockTimeline.fromNbt(timeline.toNbt(), MAX_EVENTS);
		assertEquals(timeline.size(), restored.size());
		assertEquals(timeline.newStateAt(0), restored.newStateAt(0));
	}

	@Test
	@DisplayName("entity keyframes are stored and reloaded without losing motion")
	void entityKeyframesRoundTrip() {
		EntityTimeline timeline = new EntityTimeline(16);
		EntityTrack track = timeline.createTrack(Identifier.withDefaultNamespace("zombie"));
		track.addKeyframe(0, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0.0F, true, 0, false);
		track.addKeyframe(4, 1.5D, 0.25D, -2.0D, 90.0F, 10.0F, 95.0F, false, 1, true);
		track.addAction(4, EchoEntityAction.SWING);
		timeline.internItem(Identifier.withDefaultNamespace("iron_sword"));
		track.addEquipment(0, 0, 0);

		EntityTimeline restored = EntityTimeline.fromNbt(timeline.toNbt(), 16);
		EntityTrack restoredTrack = restored.tracks().getFirst();

		assertEquals(2, restoredTrack.keyframeCount());
		assertEquals(4, restoredTrack.keyframeTick(1));
		assertEquals(1.5D, restoredTrack.keyframeX(1), 0.01D);
		assertEquals(-2.0D, restoredTrack.keyframeZ(1), 0.01D);
		assertEquals(90.0F, restoredTrack.keyframeYaw(1), 2.0F);
		assertEquals(1, restoredTrack.keyframePose(1));
		assertTrue(restoredTrack.keyframeBaby(1));
		assertTrue(restoredTrack.keyframeOnGround(0));
		assertEquals(EchoEntityAction.SWING, restoredTrack.actionAt(0));
		assertEquals(Identifier.withDefaultNamespace("zombie"), restored.entityTypeOf(restoredTrack));
		assertEquals(Identifier.withDefaultNamespace("iron_sword"), restored.itemAt(0));
	}

	@Test
	@DisplayName("the recorded entity limit is respected")
	void entityLimitIsEnforced() {
		EntityTimeline timeline = new EntityTimeline(2);
		assertTrue(timeline.hasCapacity());
		timeline.createTrack(Identifier.withDefaultNamespace("zombie"));
		timeline.createTrack(Identifier.withDefaultNamespace("skeleton"));
		assertTrue(!timeline.hasCapacity());
		assertEquals(2, timeline.trackCount());
	}

	@Test
	@DisplayName("interpolation between neighbouring keyframes does not teleport")
	void neighbouringKeyframesStayClose() {
		EntityTimeline timeline = new EntityTimeline(4);
		EntityTrack track = timeline.createTrack(Identifier.withDefaultNamespace("zombie"));
		double previousX = 0.0D;

		for (int tick = 0; tick <= 40; tick += 4) {
			double x = tick * 0.2D;
			track.addKeyframe(tick, x, 0.0D, 0.0D, 0.0F, 0.0F, 0.0F, true, 0, false);

			if (tick > 0) {
				assertTrue(Math.abs(x - previousX) < 8.0D, "keyframe jump was too large");
			}
			previousX = x;
		}

		assertNotEquals(0, track.keyframeCount());
	}
}
