package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import dev.fimkov.echoesbeneath.memory.EchoFormat;
import dev.fimkov.echoesbeneath.memory.EchoMemoryRecord;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReferences;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockPalette;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockSnapshot;
import dev.fimkov.echoesbeneath.memory.snapshot.SnapshotSection;
import dev.fimkov.echoesbeneath.memory.storage.EchoMemoryStorage;
import dev.fimkov.echoesbeneath.memory.timeline.BlockTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.EchoEntityAction;
import dev.fimkov.echoesbeneath.memory.timeline.EntityTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.EntityTrack;
import dev.fimkov.echoesbeneath.memory.timeline.ParticleTimeline;
import dev.fimkov.echoesbeneath.memory.timeline.SoundTimeline;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoPersistenceTest {
	private static final UUID RECORDED_PLAYER = UUID.nameUUIDFromBytes("echo-test-player".getBytes());
	private static final int MAX_EVENTS = 4096;
	private static final int SLOT = 1;

	private static final int RECORDED_VISION = 2;

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	@DisplayName("a block palette keeps distinct states apart and reloads them")
	void blockPaletteRoundTrips() {
		BlockPalette palette = new BlockPalette();
		int air = palette.indexOf(Blocks.AIR.defaultBlockState());
		int deepslate = palette.indexOf(Blocks.DEEPSLATE_BRICKS.defaultBlockState());
		int sculk = palette.indexOf(Blocks.SCULK.defaultBlockState());

		assertEquals(BlockPalette.AIR_INDEX, air);
		assertTrue(deepslate != sculk);
		assertEquals(deepslate, palette.indexOf(Blocks.DEEPSLATE_BRICKS.defaultBlockState()));

		BlockPalette restored = BlockPalette.fromNbt(palette.toNbt(), BuiltInRegistries.BLOCK.asLookup());
		assertEquals(Blocks.SCULK.defaultBlockState(), restored.stateAt(sculk));
		assertEquals(Blocks.AIR.defaultBlockState(), restored.stateAt(BlockPalette.AIR_INDEX));
	}

	@Test
	@DisplayName("a packed snapshot section reproduces every stored block")
	void snapshotSectionRoundTrips() {
		BlockPalette palette = new BlockPalette();
		int stone = palette.indexOf(Blocks.STONE.defaultBlockState());
		int sculk = palette.indexOf(Blocks.SCULK.defaultBlockState());

		int[] cells = new int[SnapshotSection.CELLS_PER_SECTION];
		cells[SnapshotSection.cellIndex(0, 0, 0)] = stone;
		cells[SnapshotSection.cellIndex(5, 9, 12)] = sculk;
		cells[SnapshotSection.cellIndex(15, 15, 15)] = stone;

		SnapshotSection section = SnapshotSection.compact(cells);
		SnapshotSection restored = SnapshotSection.fromNbt(section.toNbt());

		assertEquals(stone, restored.globalIndexAt(0, 0, 0));
		assertEquals(sculk, restored.globalIndexAt(5, 9, 12));
		assertEquals(stone, restored.globalIndexAt(15, 15, 15));
		assertEquals(BlockPalette.AIR_INDEX, restored.globalIndexAt(1, 1, 1));
	}

	@Test
	@DisplayName("a full memory record survives being written to disk and read back")
	void memoryRecordSurvivesAServerRestart(@TempDir Path directory) {
		EchoMemoryRecord original = sampleRecord(UUID.randomUUID());
		EchoMemoryStorage storage = new EchoMemoryStorage(directory, BuiltInRegistries.BLOCK.asLookup());
		assertTrue(storage.save(original));

		storage.clearCache();
		EchoMemoryRecord reloaded = storage.load(original.id()).orElseThrow();

		assertEquals(original.id(), reloaded.id());
		assertEquals(original.slot(), reloaded.slot());
		assertEquals(original.dimension(), reloaded.dimension());
		assertEquals(original.center(), reloaded.center());
		assertEquals(original.radius(), reloaded.radius());
		assertEquals(original.verticalRadius(), reloaded.verticalRadius());
		assertEquals(original.durationTicks(), reloaded.durationTicks());
		assertEquals(original.formatVersion(), reloaded.formatVersion());

		assertEquals(Blocks.DEEPSLATE_BRICKS.defaultBlockState(), reloaded.snapshot().stateAt(2, 3, 4));
		assertEquals(Blocks.AIR.defaultBlockState(), reloaded.snapshot().stateAt(9, 9, 9));

		assertEquals(1, reloaded.soundTimeline().size());
		assertEquals(120, reloaded.soundTimeline().tickAt(0));
		assertEquals(ResourceLocation.withDefaultNamespace("entity.zombie.ambient"),
				reloaded.soundTimeline().soundAt(0));
		assertEquals(4.0D, reloaded.soundTimeline().relativeXAt(0), 0.01D);
		assertEquals(-8.0D, reloaded.soundTimeline().relativeZAt(0), 0.01D);

		assertEquals(1, reloaded.blockTimeline().size());
		assertEquals(1, reloaded.entityTimeline().trackCount());
		assertEquals(EchoEntityAction.HURT, reloaded.entityTimeline().tracks().getFirst().actionAt(0));
	}

	@Test
	@DisplayName("playing a memory back never rewrites it")
	void loadingDoesNotMutateStoredData(@TempDir Path directory) {
		EchoMemoryRecord original = sampleRecord(UUID.randomUUID());
		EchoMemoryStorage storage = new EchoMemoryStorage(directory, BuiltInRegistries.BLOCK.asLookup());
		storage.save(original);

		for (int replay = 0; replay < 4; replay++) {
			storage.clearCache();
			EchoMemoryRecord reloaded = storage.load(original.id()).orElseThrow();
			assertEquals(original.durationTicks(), reloaded.durationTicks());
			assertEquals(1, reloaded.soundTimeline().size());
		}
		assertTrue(storage.exists(original.id()));
	}

	@Test
	@DisplayName("a memory knows which players it caught, and does not count the one asking")
	void recordedPlayersSurviveAndExcludeTheAsker() {
		EchoMemoryRecord record = sampleRecord(UUID.randomUUID());
		assertTrue(record.holdsPlayerOtherThan(UUID.randomUUID()),
				"a memory holding a stranger must report one");
		assertFalse(record.holdsPlayerOtherThan(RECORDED_PLAYER),
				"a memory holding only you must not count you as somebody else");
		EchoMemoryRecord restored = EchoMemoryRecord.fromNbt(record.toNbt(),
				BuiltInRegistries.BLOCK.asLookup(), MAX_EVENTS, MAX_EVENTS, MAX_EVENTS, MAX_EVENTS);
		assertEquals(record.recordedPlayers(), restored.recordedPlayers());
	}

	@Test
	@DisplayName("a record from an unsupported format is refused instead of crashing the world")
	void unsupportedFormatIsRefused() {
		CompoundTag tag = sampleRecord(UUID.randomUUID()).toNbt();
		tag.putInt("format_version", EchoFormat.CURRENT_VERSION + 99);
		assertThrows(RuntimeException.class,
				() -> EchoMemoryRecord.fromNbt(tag, BuiltInRegistries.BLOCK.asLookup(),
						MAX_EVENTS, MAX_EVENTS, MAX_EVENTS, MAX_EVENTS));
	}

	@Test
	@DisplayName("a damaged record is skipped rather than propagated")
	void damagedRecordIsSkipped(@TempDir Path directory) {
		EchoMemoryStorage storage = new EchoMemoryStorage(directory, BuiltInRegistries.BLOCK.asLookup());
		Optional<EchoMemoryRecord> missing = storage.load(UUID.randomUUID());
		assertTrue(missing.isEmpty());
	}

	@Test
	@DisplayName("all three memory identities survive item serialisation")
	void itemComponentKeepsAllThreeIdentities() {
		EchoMemoryReferences references = EchoMemorySlotTest.filledLens();
		DataResult<Tag> encoded = EchoMemoryReferences.CODEC.encodeStart(NbtOps.INSTANCE, references);
		Tag tag = encoded.getOrThrow();
		EchoMemoryReferences decoded = EchoMemoryReferences.CODEC
				.parse(NbtOps.INSTANCE, tag).getOrThrow();

		assertEquals(3, decoded.filledSlots());
		List<EchoMemoryReference> originalEntries = references.entries();
		for (EchoMemoryReference reference : originalEntries) {
			EchoMemoryReference match = decoded.findBySlot(reference.slot()).orElseThrow();
			assertEquals(reference.memoryId(), match.memoryId());
			assertEquals(reference.dimension(), match.dimension());
			assertEquals(reference.center(), match.center());
			assertEquals(reference.durationTicks(), match.durationTicks());
		}
		assertFalse(decoded.isEmpty());
	}

	@Test
	@DisplayName("the recovery command removes exactly one memory")
	void recoveryRemovesOnlyTheNamedMemory() {
		EchoMemoryReferences references = EchoMemorySlotTest.filledLens();
		UUID second = references.findBySlot(1).orElseThrow().memoryId();
		EchoMemoryReferences reduced = references.withRemoved(second);
		assertEquals(2, reduced.filledSlots());
		assertTrue(reduced.findById(second).isEmpty());
		assertTrue(reduced.findBySlot(0).isPresent());
		assertTrue(reduced.findBySlot(2).isPresent());
	}

	private static EchoMemoryRecord sampleRecord(UUID id) {
		BlockPalette palette = new BlockPalette();
		int bricks = palette.indexOf(Blocks.DEEPSLATE_BRICKS.defaultBlockState());
		int[] cells = new int[SnapshotSection.CELLS_PER_SECTION];
		cells[SnapshotSection.cellIndex(2, 3, 4)] = bricks;
		Int2ObjectMap<SnapshotSection> sections = new Int2ObjectOpenHashMap<>();
		sections.put(BlockSnapshot.sectionKey(0, 0, 0), SnapshotSection.compact(cells));
		BlockSnapshot snapshot = new BlockSnapshot(palette, sections, 50, -32, 32);

		BlockTimeline blockTimeline = new BlockTimeline(MAX_EVENTS);
		blockTimeline.record(60, EchoCoordinates.packRelative(1, 0, 1),
				BlockPalette.AIR_INDEX, bricks);

		EntityTimeline entityTimeline = new EntityTimeline(64);
		EntityTrack track = entityTimeline.createTrack(ResourceLocation.withDefaultNamespace("zombie"));
		track.addKeyframe(0, 1.0D, 0.0D, 1.0D, 45.0F, 0.0F, 45.0F, true, 0, false);
		track.addAction(20, EchoEntityAction.HURT);

		SoundTimeline soundTimeline = new SoundTimeline(MAX_EVENTS);
		soundTimeline.record(120, 4.0D, 1.0D, -8.0D,
				ResourceLocation.withDefaultNamespace("entity.zombie.ambient"),
				SoundSource.HOSTILE, 1.0F, 0.95F);

		ParticleTimeline particleTimeline = new ParticleTimeline(MAX_EVENTS);
		particleTimeline.record(120, 4.0D, 1.0D, -8.0D,
				ResourceLocation.withDefaultNamespace("sculk_soul"), 3, 0.2D, 0.1D, 0.2D);

		return new EchoMemoryRecord(id, SLOT, EchoFormat.CURRENT_VERSION, EchoTestSupport.OVERWORLD,
				new BlockPos(120, 40, -60), 50, 32, 1_700_000_000_000L, 2400,
				snapshot, blockTimeline, entityTimeline, soundTimeline, particleTimeline,
				List.of(RECORDED_PLAYER), RECORDED_VISION);
	}

	@Test
	@DisplayName("a memory keeps the vision the lens had when it was sealed")
	void visionSurvivesTheRoundTrip(@TempDir Path directory) {
		EchoMemoryStorage storage = new EchoMemoryStorage(directory, BuiltInRegistries.BLOCK.asLookup());
		EchoMemoryRecord record = sampleRecord(UUID.randomUUID());
		storage.save(record);
		EchoMemoryRecord reloaded = storage.load(record.id()).orElseThrow();
		assertEquals(RECORDED_VISION, reloaded.recordedVision());
	}
}
