package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.memory.EchoFormat;
import dev.fimkov.echoesbeneath.menu.ResonanceTablePlan;
import dev.fimkov.echoesbeneath.menu.ResonanceTableUpgrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResonanceTablePlanTest {
	private static ResonanceTablePlan plan(EchoLensUpgrades current, EchoLensUpgrades peak,
			EchoLensUpgrades desired) {
		return ResonanceTablePlan.of(current, ResonanceTableUpgrade.lowestSetting(0), peak, desired);
	}

	@Test
	@DisplayName("a plan that changes nothing costs nothing")
	void emptyPlanIsFree() {
		ResonanceTablePlan result = plan(EchoLensUpgrades.DEFAULT, EchoLensUpgrades.DEFAULT,
				EchoLensUpgrades.DEFAULT);
		assertFalse(result.changed());
		assertTrue(result.isFree());
	}

	@Test
	@DisplayName("winding a track back down is free")
	void downgradesCostNothing() {
		EchoLensUpgrades tuned = new EchoLensUpgrades(1, 48, 3, 2);
		ResonanceTablePlan result = plan(tuned, tuned, EchoLensUpgrades.DEFAULT);
		assertTrue(result.changed());
		assertTrue(result.isFree());
		assertEquals(EchoLensUpgrades.MIN_RADIUS, result.target().radius());
		assertEquals(0, result.target().clarity());
	}

	@Test
	@DisplayName("climbing back to a level this lens already reached is free")
	void restoringToThePeakCostsNothing() {
		EchoLensUpgrades peak = new EchoLensUpgrades(1, 48, 3, 2);
		EchoLensUpgrades current = EchoLensUpgrades.DEFAULT;
		ResonanceTablePlan result = plan(current, peak, peak);
		assertTrue(result.changed());
		assertTrue(result.isFree());
		assertEquals(peak, result.target());
	}

	@Test
	@DisplayName("only the steps past the peak are charged for")
	void onlyNewGroundIsCharged() {
		EchoLensUpgrades peak = ResonanceTableUpgrade.RADIUS.withSteps(EchoLensUpgrades.DEFAULT, 2);
		EchoLensUpgrades desired = ResonanceTableUpgrade.RADIUS.withSteps(EchoLensUpgrades.DEFAULT, 5);
		ResonanceTablePlan result = plan(EchoLensUpgrades.DEFAULT, peak, desired);
		assertEquals(3 * ResonanceTableUpgrade.RADIUS.experiencePerStep(), result.experienceLevels());
	}

	@Test
	@DisplayName("memory slots never wind back below the memories already sealed")
	void sealedMemoriesHoldTheirSlots() {
		EchoLensUpgrades floor = ResonanceTableUpgrade.lowestSetting(2);
		EchoLensUpgrades owned = new EchoLensUpgrades(3, 16, 0, 0);
		ResonanceTablePlan result = ResonanceTablePlan.of(owned, floor, owned, EchoLensUpgrades.DEFAULT);
		assertEquals(2, result.target().memorySlots());
		assertTrue(result.isFree());
	}

	@Test
	@DisplayName("each staged step adds that upgrade's own price")
	void costsAccumulatePerStep() {
		EchoLensUpgrades target = ResonanceTableUpgrade.RADIUS.withSteps(EchoLensUpgrades.DEFAULT, 3);
		ResonanceTablePlan result = plan(EchoLensUpgrades.DEFAULT, EchoLensUpgrades.DEFAULT, target);
		assertTrue(result.changed());
		assertEquals(3 * ResonanceTableUpgrade.RADIUS.experiencePerStep(), result.experienceLevels());
		assertEquals(0, result.amethystShards());
		assertEquals(EchoLensUpgrades.MIN_RADIUS + 3 * EchoLensUpgrades.RADIUS_STEP, result.target().radius());
	}

	@Test
	@DisplayName("memory slots are the only upgrade that also eats amethyst")
	void memorySlotsCostShards() {
		EchoLensUpgrades target = ResonanceTableUpgrade.MEMORY_SLOT.withSteps(EchoLensUpgrades.DEFAULT, 2);
		ResonanceTablePlan result = plan(EchoLensUpgrades.DEFAULT, EchoLensUpgrades.DEFAULT, target);
		assertEquals(2 * ResonanceTableUpgrade.MEMORY_SLOT.experiencePerStep(), result.experienceLevels());
		assertEquals(2 * ResonanceTableUpgrade.MEMORY_SLOT.shardsPerStep(), result.amethystShards());
		assertEquals(EchoFormat.MAX_MEMORY_SLOTS, result.target().memorySlots());
	}

	@Test
	@DisplayName("clarity is the dearest upgrade on the table")
	void clarityCostsTheMost() {
		for (ResonanceTableUpgrade upgrade : ResonanceTableUpgrade.all()) {
			if (upgrade == ResonanceTableUpgrade.CLARITY) {
				continue;
			}
			assertTrue(ResonanceTableUpgrade.CLARITY.experiencePerStep() > upgrade.experiencePerStep(),
					upgrade + " costs at least as much as clarity");
		}
	}

	@Test
	@DisplayName("a floor of its own settings pins a plan in place")
	void plansCannotUndoOwnedUpgrades() {
		EchoLensUpgrades owned = new EchoLensUpgrades(2, 32, 3, 2);
		ResonanceTablePlan result = ResonanceTablePlan.of(owned, owned, owned, EchoLensUpgrades.DEFAULT);
		assertFalse(result.changed());
		assertEquals(owned, result.target());
		assertEquals(0, result.experienceLevels());
	}

	@Test
	@DisplayName("staged values are capped at each upgrade's ceiling")
	void plansCannotExceedTheCeiling() {
		EchoLensUpgrades wild = new EchoLensUpgrades(EchoFormat.MAX_MEMORY_SLOTS, EchoLensUpgrades.MAX_RADIUS,
				EchoLensUpgrades.MAX_SENSITIVITY, EchoLensUpgrades.MAX_CLARITY);
		EchoLensUpgrades clamped = ResonanceTableUpgrade.clampToFloor(EchoLensUpgrades.DEFAULT, wild);
		for (ResonanceTableUpgrade upgrade : ResonanceTableUpgrade.all()) {
			assertEquals(upgrade.maxSteps(), upgrade.stepsOf(clamped), upgrade + " did not reach its ceiling");
			assertEquals(upgrade.maxSteps(), upgrade.stepsOf(upgrade.withSteps(clamped, upgrade.maxSteps() + 5)),
					upgrade + " went past its ceiling");
		}
	}

	@Test
	@DisplayName("button ids round-trip to a row and a direction")
	void buttonIdsRoundTrip() {
		for (int index = 0; index < ResonanceTableUpgrade.all().length; index++) {
			int increase = ResonanceTableUpgrade.buttonId(index, true);
			int decrease = ResonanceTableUpgrade.buttonId(index, false);
			assertEquals(index, ResonanceTableUpgrade.indexOfButton(increase));
			assertEquals(index, ResonanceTableUpgrade.indexOfButton(decrease));
			assertTrue(ResonanceTableUpgrade.isIncrease(increase));
			assertFalse(ResonanceTableUpgrade.isIncrease(decrease));
		}
	}
}
