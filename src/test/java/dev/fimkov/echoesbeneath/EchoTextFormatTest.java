package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.memory.EchoTextFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EchoTextFormatTest {
	@Test
	@DisplayName("memory slots read as I, II and III")
	void slotsUseRomanNumerals() {
		assertEquals("I", EchoTextFormat.romanSlot(0));
		assertEquals("II", EchoTextFormat.romanSlot(1));
		assertEquals("III", EchoTextFormat.romanSlot(2));
	}

	@Test
	@DisplayName("the action bar clock matches the documented format")
	void clockMatchesTheActionBarFormat() {
		assertEquals("00:00", EchoTextFormat.clock(0));
		assertEquals("01:17", EchoTextFormat.clock(77 * 20));
		assertEquals("02:00", EchoTextFormat.clock(2400));
		assertEquals("01:42", EchoTextFormat.clock(102 * 20));
	}
}
