package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.memory.EchoFormat;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReferences;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoMemorySlotTest {
	@Test
	@DisplayName("a fresh lens holds no memories")
	void freshLensIsEmpty() {
		assertEquals(0, EchoMemoryReferences.EMPTY.filledSlots());
		assertEquals(EchoFormat.MEMORY_SLOT_COUNT, 3);
		assertTrue(EchoMemoryReferences.EMPTY.isEmpty());
		assertFalse(EchoMemoryReferences.EMPTY.isFull());
	}

	@Test
	@DisplayName("recordings fill Memory I, then II, then III in order")
	void slotsFillInOrder() {
		EchoMemoryReferences references = EchoMemoryReferences.EMPTY;
		assertEquals(0, references.nextFreeSlot().orElseThrow());

		references = references.withAdded(EchoTestSupport.reference(0, BlockPos.ZERO));
		assertEquals(1, references.nextFreeSlot().orElseThrow());
		assertEquals(1, references.filledSlots());

		references = references.withAdded(EchoTestSupport.reference(1, new BlockPos(500, 64, 0)));
		assertEquals(2, references.nextFreeSlot().orElseThrow());
		assertEquals(2, references.filledSlots());

		references = references.withAdded(EchoTestSupport.reference(2, new BlockPos(1000, 64, 0)));
		assertTrue(references.nextFreeSlot().isEmpty());
		assertEquals(3, references.filledSlots());
		assertTrue(references.isFull());
	}

	@Test
	@DisplayName("a fourth recording is rejected because no slot is free")
	void fourthRecordingIsRejected() {
		EchoMemoryReferences full = filledLens();
		assertTrue(full.isFull());
		assertTrue(full.nextFreeSlot().isEmpty());
	}

	@Test
	@DisplayName("a sealed slot cannot be overwritten")
	void sealedSlotCannotBeOverwritten() {
		EchoMemoryReferences references = EchoMemoryReferences.EMPTY
				.withAdded(EchoTestSupport.reference(0, BlockPos.ZERO));
		assertThrows(IllegalStateException.class,
				() -> references.withAdded(EchoTestSupport.reference(0, new BlockPos(9, 9, 9))));
	}

	@Test
	@DisplayName("reading a memory leaves the reference list untouched")
	void readingDoesNotConsume() {
		EchoMemoryReferences full = filledLens();
		UUID second = full.findBySlot(1).orElseThrow().memoryId();
		for (int repeat = 0; repeat < 5; repeat++) {
			assertEquals(second, full.findBySlot(1).orElseThrow().memoryId());
			assertEquals(3, full.filledSlots());
		}
	}

	@Test
	@DisplayName("the entry list is immutable")
	void entriesAreImmutable() {
		EchoMemoryReferences references = EchoMemoryReferences.EMPTY
				.withAdded(EchoTestSupport.reference(0, BlockPos.ZERO));
		assertThrows(UnsupportedOperationException.class,
				() -> references.entries().add(EchoTestSupport.reference(1, BlockPos.ZERO)));
	}

	@Test
	@DisplayName("slot indices map to Memory I, II and III")
	void slotDisplayIndices() {
		assertEquals(1, EchoTestSupport.reference(0, BlockPos.ZERO).displayIndex());
		assertEquals(2, EchoTestSupport.reference(1, BlockPos.ZERO).displayIndex());
		assertEquals(3, EchoTestSupport.reference(2, BlockPos.ZERO).displayIndex());
	}

	static EchoMemoryReferences filledLens() {
		return EchoMemoryReferences.EMPTY
				.withAdded(EchoTestSupport.reference(0, BlockPos.ZERO))
				.withAdded(EchoTestSupport.reference(1, new BlockPos(500, 64, 0)))
				.withAdded(EchoTestSupport.reference(2, new BlockPos(1000, 64, 0)));
	}
}
