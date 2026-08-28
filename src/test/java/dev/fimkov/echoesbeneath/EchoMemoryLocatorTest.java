package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.memory.EchoMemoryLocator;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReferences;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoMemoryLocatorTest {
	@Test
	@DisplayName("a memory records the dimension it was taken in")
	void memoryKeepsItsDimension() {
		EchoMemoryReference reference = EchoTestSupport.reference(0, EchoTestSupport.NETHER, BlockPos.ZERO);
		assertEquals(EchoTestSupport.NETHER, reference.dimension());
	}

	@Test
	@DisplayName("a memory does not resonate in another dimension")
	void otherDimensionDoesNotResonate() {
		EchoMemoryReferences references = EchoMemoryReferences.EMPTY
				.withAdded(EchoTestSupport.reference(0, EchoTestSupport.OVERWORLD, BlockPos.ZERO));
		assertTrue(EchoMemoryLocator.resonating(references, EchoTestSupport.NETHER, BlockPos.ZERO).isEmpty());
	}

	@Test
	@DisplayName("a memory does not resonate far away from its centre")
	void distantPositionDoesNotResonate() {
		EchoMemoryReferences references = EchoMemoryReferences.EMPTY
				.withAdded(EchoTestSupport.reference(0, BlockPos.ZERO));
		assertTrue(EchoMemoryLocator.resonating(references, EchoTestSupport.OVERWORLD,
				new BlockPos(400, 0, 0)).isEmpty());
	}

	@Test
	@DisplayName("a memory resonates inside its own radius")
	void nearbyPositionResonates() {
		EchoMemoryReferences references = EchoMemoryReferences.EMPTY
				.withAdded(EchoTestSupport.reference(0, BlockPos.ZERO));
		Optional<EchoMemoryReference> found = EchoMemoryLocator.resonating(
				references, EchoTestSupport.OVERWORLD, new BlockPos(20, 4, 10));
		assertTrue(found.isPresent());
		assertEquals(0, found.orElseThrow().slot());
	}

	@Test
	@DisplayName("the vertical range is respected")
	void verticalRangeIsRespected() {
		EchoMemoryReferences references = EchoMemoryReferences.EMPTY
				.withAdded(EchoTestSupport.reference(0, BlockPos.ZERO));
		assertTrue(EchoMemoryLocator.resonating(references, EchoTestSupport.OVERWORLD,
				new BlockPos(0, EchoTestSupport.DEFAULT_VERTICAL_RADIUS, 0)).isPresent());
		assertTrue(EchoMemoryLocator.resonating(references, EchoTestSupport.OVERWORLD,
				new BlockPos(0, EchoTestSupport.DEFAULT_VERTICAL_RADIUS + 1, 0)).isEmpty());
	}

	@Test
	@DisplayName("overlapping memories resolve to the nearest centre")
	void overlappingMemoriesPickTheNearestCentre() {
		EchoMemoryReferences references = EchoMemoryReferences.EMPTY
				.withAdded(EchoTestSupport.reference(0, new BlockPos(0, 0, 0)))
				.withAdded(EchoTestSupport.reference(1, new BlockPos(30, 0, 0)));
		Optional<EchoMemoryReference> found = EchoMemoryLocator.resonating(
				references, EchoTestSupport.OVERWORLD, new BlockPos(28, 0, 0));
		assertEquals(1, found.orElseThrow().slot());
	}

	@Test
	@DisplayName("an equal distance resolves to the lower memory number")
	void equalDistancePicksTheLowerSlot() {
		EchoMemoryReferences references = EchoMemoryReferences.EMPTY
				.withAdded(EchoTestSupport.reference(1, new BlockPos(20, 0, 0)))
				.withAdded(EchoTestSupport.reference(0, new BlockPos(-20, 0, 0)));
		Optional<EchoMemoryReference> found = EchoMemoryLocator.resonating(
				references, EchoTestSupport.OVERWORLD, BlockPos.ZERO);
		assertEquals(0, found.orElseThrow().slot());
	}

	@Test
	@DisplayName("a memory nested in a wider one wins where the two overlap")
	void nestedMemoryBeatsTheWiderOne() {
		EchoMemoryReferences references = EchoMemoryReferences.EMPTY
				.withAdded(EchoTestSupport.reference(0, new BlockPos(8, 0, 0), 50, 32))
				.withAdded(EchoTestSupport.reference(1, BlockPos.ZERO, 8, 8));
		Optional<EchoMemoryReference> found = EchoMemoryLocator.resonating(
				references, EchoTestSupport.OVERWORLD, new BlockPos(7, 0, 0));
		assertEquals(1, found.orElseThrow().slot());
	}
}
