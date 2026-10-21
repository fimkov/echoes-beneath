package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.chart.EchoChartShape;
import dev.fimkov.echoesbeneath.chart.EchoSounding;
import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import dev.fimkov.echoesbeneath.memory.EchoFormat;
import dev.fimkov.echoesbeneath.memory.EchoMemoryRecord;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockPalette;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockSnapshot;
import dev.fimkov.echoesbeneath.memory.snapshot.SnapshotSection;
import dev.fimkov.echoesbeneath.memory.timeline.BlockTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.EntityTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.ParticleTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.SoundTimeline;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoSoundingTest {
	private static final int RADIUS = 12;
	private static final int VERTICAL = 12;
	private static final BlockPos CENTRE = new BlockPos(0, 64, 0);
	private static final int BUDGET = 4096;
	private static final int RECORDED_VISION = 2;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static BlockSnapshot stoneWithRooms() {
		return roomsIn(Blocks.STONE.defaultBlockState());
	}

	private static BlockSnapshot roomsIn(net.minecraft.world.level.block.state.BlockState material) {
		BlockPalette palette = new BlockPalette();
		int stone = palette.indexOf(material);
		Int2ObjectMap<SnapshotSection> sections = new Int2ObjectOpenHashMap<>();

		for (int sectionX = -1; sectionX <= 0; sectionX++) {
			for (int sectionY = -1; sectionY <= 0; sectionY++) {
				for (int sectionZ = -1; sectionZ <= 0; sectionZ++) {
					int[] cells = new int[SnapshotSection.CELLS_PER_SECTION];

					for (int cell = 0; cell < cells.length; cell++) {
						cells[cell] = stone;
					}
					for (int localY = 0; localY < 16; localY++) {
						for (int localZ = 0; localZ < 16; localZ++) {
							for (int localX = 0; localX < 16; localX++) {
								int x = sectionX * 16 + localX;
								int y = sectionY * 16 + localY;
								int z = sectionZ * 16 + localZ;

								if (inOpenRoom(x, y, z) || inSealedRoom(x, y, z) || inUpperVoid(x, y, z)) {
									cells[SnapshotSection.cellIndex(localX, localY, localZ)] =
											BlockPalette.AIR_INDEX;
								}
							}
						}
					}
					sections.put(BlockSnapshot.sectionKey(sectionX, sectionY, sectionZ),
							SnapshotSection.compact(cells));
				}
			}
		}

		return new BlockSnapshot(palette, sections, RADIUS, -VERTICAL, VERTICAL);
	}

	private static boolean inOpenRoom(int x, int y, int z) {
		return Math.abs(x) <= 2 && Math.abs(y) <= 2 && Math.abs(z) <= 2;
	}

	private static boolean inUpperVoid(int x, int y, int z) {
		return Math.abs(x) <= 2 && y >= 7 && y <= 9 && Math.abs(z) <= 2;
	}

	private static boolean inSealedRoom(int x, int y, int z) {
		return x >= 7 && x <= 9 && y >= -1 && y <= 1 && z >= -1 && z <= 1;
	}

	private static EchoMemoryRecord recordWith(SoundTimeline sounds) {
		return recordWith(sounds, stoneWithRooms());
	}

	private static EchoMemoryRecord recordWith(SoundTimeline sounds, BlockSnapshot snapshot) {
		return new EchoMemoryRecord(UUID.randomUUID(), 0, EchoFormat.CURRENT_VERSION,
				EchoTestSupport.OVERWORLD, CENTRE, RADIUS, VERTICAL, 1_700_000_000_000L, 200,
				snapshot, new BlockTimeline(256), new EntityTimeline(16), sounds,
				new ParticleTimeline(16), List.of(), RECORDED_VISION);
	}

	private static SoundTimeline oneSoundInTheOpenRoom() {
		SoundTimeline sounds = new SoundTimeline(64);
		sounds.record(10, 0.5D, 0.5D, 0.5D, ResourceLocation.withDefaultNamespace("block.stone.break"),
				SoundSource.BLOCKS, 1.0F, 1.0F);

		return sounds;
	}

	@Test
	@DisplayName("a sound lights up the walls of the room it was made in")
	void soundReachesItsOwnRoom() {
		EchoChartShape shape = EchoSounding.of(recordWith(oneSoundInTheOpenRoom()),
				Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2, BUDGET);
		assertFalse(shape.isEmpty(), "the open room should have walls");
		assertTrue(holds(shape, 3, 0, 0), "the room's own wall should be charted");
	}

	@Test
	@DisplayName("a sound never reaches across a column of solid stone")
	void soundStopsAtStone() {
		EchoChartShape shape = EchoSounding.of(recordWith(oneSoundInTheOpenRoom()),
				Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2, BUDGET);
		assertFalse(holds(shape, 8, 0, 0), "the sealed room must stay dark");
		assertFalse(holds(shape, 10, 0, 0), "nothing past the sealed room may be charted");
	}

	@Test
	@DisplayName("a column the sound reached is charted top to bottom, not only beside the sound")
	void reachedColumnsGoAllTheWayDown() {
		EchoChartShape shape = EchoSounding.of(recordWith(oneSoundInTheOpenRoom()),
				Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2, BUDGET);
		int lowestCharted = Integer.MAX_VALUE;
		int highestCharted = Integer.MIN_VALUE;

		for (int index = 0; index < shape.size(); index++) {
			int y = EchoCoordinates.unpackY(shape.positions().getInt(index));
			lowestCharted = Math.min(lowestCharted, y);
			highestCharted = Math.max(highestCharted, y);
		}

		assertTrue(holds(shape, 0, 6, 0),
				"the floor of the void above should be charted, though no sound could reach it");
		assertTrue(highestCharted - lowestCharted > 6,
				"the chart spans only " + (highestCharted - lowestCharted) + " blocks of height");
	}

	@Test
	@DisplayName("a chart remembers how much the recording lens could see")
	void chartKeepsTheRecordedVision() {
		EchoChartShape shape = EchoSounding.of(recordWith(oneSoundInTheOpenRoom()),
				Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2, BUDGET);
		assertEquals(RECORDED_VISION, shape.recordedVision());
	}

	@Test
	@DisplayName("asking for more height than was recorded gives back only what was recorded")
	void windowCannotOutgrowTheRecording() {
		EchoChartShape wide = EchoSounding.of(recordWith(oneSoundInTheOpenRoom()),
				CENTRE.getY() - 400, CENTRE.getY() + 400, BUDGET);
		EchoChartShape exact = EchoSounding.of(recordWith(oneSoundInTheOpenRoom()),
				CENTRE.getY() - VERTICAL, CENTRE.getY() + VERTICAL, BUDGET);
		assertEquals(exact.size(), wide.size());
		assertEquals(-VERTICAL, wide.lowestRelativeY());
		assertEquals(VERTICAL, wide.highestRelativeY());
	}

	@Test
	@DisplayName("silence charts nothing at all")
	void silenceChartsNothing() {
		EchoChartShape shape = EchoSounding.of(recordWith(new SoundTimeline(64)),
				Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2, BUDGET);
		assertTrue(shape.isEmpty());
	}

	@Test
	@DisplayName("the height window keeps out everything above and below it")
	void heightWindowClips() {
		EchoChartShape shape = EchoSounding.of(recordWith(oneSoundInTheOpenRoom()),
				CENTRE.getY(), CENTRE.getY(), BUDGET);
		assertFalse(shape.isEmpty());

		for (int index = 0; index < shape.size(); index++) {
			assertEquals(0, EchoCoordinates.unpackY(shape.positions().getInt(index)));
		}
	}

	@Test
	void sphericalChartsClipCornersButKeepTheNearbyRoom() {
		EchoMemoryRecord original = recordWith(oneSoundInTheOpenRoom());
		EchoMemoryRecord record = new EchoMemoryRecord(original.id(), original.slot(), original.formatVersion(),
				original.dimension(), original.center(), 4, original.verticalRadius(), original.createdAtEpochMillis(),
				original.durationTicks(), original.snapshot(), original.blockTimeline(), original.entityTimeline(),
				original.soundTimeline(), original.particleTimeline(), original.recordedPlayers(), original.recordedVision());
		var reach = EchoSounding.reach(record);
		EchoChartShape cylinder = EchoSounding.of(record, reach, 0, 128, BUDGET, false);
		EchoChartShape sphere = EchoSounding.of(record, reach, 0, 128, BUDGET, true);
		assertTrue(holds(cylinder, 0, 6, 0));
		assertFalse(holds(sphere, 0, 6, 0));
		assertTrue(holds(sphere, 3, 0, 0));
		for (int position : sphere.positions()) {
			int x = EchoCoordinates.unpackX(position);
			int y = EchoCoordinates.unpackY(position);
			int z = EchoCoordinates.unpackZ(position);
			assertTrue(x * x + y * y + z * z <= record.radius() * record.radius());
		}
	}

	@Test
	void chartPacketsNeverRevealOreTypesOrUnusedPaletteEntries() {
		BlockSnapshot snapshot = roomsIn(Blocks.DIAMOND_ORE.defaultBlockState());
		snapshot.palette().indexOf(Blocks.GOLD_ORE.defaultBlockState());
		EchoChartShape shape = EchoSounding.of(recordWith(oneSoundInTheOpenRoom(), snapshot), 0, 128, BUDGET);
		assertFalse(shape.isEmpty());
		var decoded = dev.fimkov.echoesbeneath.chart.EchoChartCodec.decode(
				dev.fimkov.echoesbeneath.chart.EchoChartCodec.encode(shape));
		assertEquals(List.of(Blocks.AIR.defaultBlockState(), Blocks.STONE.defaultBlockState()), decoded.palette());
	}

	@Test
	void stoppingAJukeboxDoesNotChartANewRoom() {
		SoundTimeline sounds = new SoundTimeline(16);
		sounds.recordJukebox(4, 0.5D, 0.5D, 0.5D,
				ResourceLocation.withDefaultNamespace("intentionally_empty"), false);
		assertTrue(EchoSounding.of(recordWith(sounds), 0, 128, BUDGET).isEmpty());
	}

	private static boolean holds(EchoChartShape shape, int x, int y, int z) {
		int wanted = EchoCoordinates.packRelative(x, y, z);

		for (int index = 0; index < shape.size(); index++) {
			if (shape.positions().getInt(index) == wanted) {
				return true;
			}
		}

		return false;
	}
}
