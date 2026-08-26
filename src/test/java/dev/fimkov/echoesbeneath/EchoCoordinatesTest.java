package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoCoordinatesTest {
	@Test
	@DisplayName("relative positions survive packing across the recorded range")
	void packedPositionsRoundTrip() {
		for (int x = -64; x <= 64; x += 7) {
			for (int y = -64; y <= 64; y += 11) {
				for (int z = -64; z <= 64; z += 13) {
					int packed = EchoCoordinates.packRelative(x, y, z);
					assertEquals(x, EchoCoordinates.unpackX(packed));
					assertEquals(y, EchoCoordinates.unpackY(packed));
					assertEquals(z, EchoCoordinates.unpackZ(packed));
				}
			}
		}
	}

	@Test
	@DisplayName("the packed range covers the configurable maximum radius")
	void packedRangeCoversMaximumRadius() {
		assertTrue(EchoCoordinates.fitsInPackedRange(64));
		assertTrue(EchoCoordinates.fitsInPackedRange(-64));
		assertFalse(EchoCoordinates.fitsInPackedRange(512));
	}

	@Test
	@DisplayName("quantised positions stay within a quarter of a pixel")
	void positionQuantisationIsPrecise() {
		double[] samples = {0.0D, 0.5D, -12.25D, 49.99D, -32.125D};

		for (double sample : samples) {
			double restored = EchoCoordinates.dequantizePosition(EchoCoordinates.quantizePosition(sample));
			assertTrue(Math.abs(restored - sample) < 0.005D, "drifted for " + sample);
		}
	}

	@Test
	@DisplayName("quantised rotations stay within two degrees")
	void rotationQuantisationIsPrecise() {
		float[] samples = {0.0F, 90.0F, -179.0F, 44.5F, 359.0F};

		for (float sample : samples) {
			float restored = EchoCoordinates.dequantizeRotation(EchoCoordinates.quantizeRotation(sample));
			assertTrue(Math.abs(restored - sample) < 2.0F, "drifted for " + sample);
		}
	}
}
