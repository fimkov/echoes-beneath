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
	@Test
	@DisplayName("a plan that changes nothing costs nothing")
	void emptyPlanIsFree() {
		ResonanceTablePlan plan = ResonanceTablePlan.between(EchoLensUpgrades.DEFAULT, EchoLensUpgrades.DEFAULT);
		assertTrue(plan.isEmpty());
		assertEquals(0, plan.experienceLevels());
		assertEquals(0, plan.amethystShards());
	}

	@Test
	@DisplayName("each staged step adds that upgrade's own price")
	void costsAccumulatePerStep() {
		EchoLensUpgrades target = ResonanceTableUpgrade.RADIUS.withSteps(EchoLensUpgrades.DEFAULT, 3);
		ResonanceTablePlan plan = ResonanceTablePlan.between(EchoLensUpgrades.DEFAULT, target);
		assertFalse(plan.isEmpty());
		assertEquals(3 * ResonanceTableUpgrade.RADIUS.experiencePerStep(), plan.experienceLevels());
		assertEquals(0, plan.amethystShards());
		assertEquals(EchoLensUpgrades.MIN_RADIUS + 3 * EchoLensUpgrades.RADIUS_STEP, plan.target().radius());
	}

	@Test
	@DisplayName("memory slots are the only upgrade that also eats amethyst")
	void memorySlotsCostShards() {
		EchoLensUpgrades target = ResonanceTableUpgrade.MEMORY_SLOT.withSteps(EchoLensUpgrades.DEFAULT, 2);
		ResonanceTablePlan plan = ResonanceTablePlan.between(EchoLensUpgrades.DEFAULT, target);
		assertEquals(2 * ResonanceTableUpgrade.MEMORY_SLOT.experiencePerStep(), plan.experienceLevels());
		assertEquals(2 * ResonanceTableUpgrade.MEMORY_SLOT.shardsPerStep(), plan.amethystShards());
		assertEquals(EchoFormat.MAX_MEMORY_SLOTS, plan.target().memorySlots());
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
	@DisplayName("staged values can never fall below what the lens already carries")
	void plansCannotUndoOwnedUpgrades() {
		EchoLensUpgrades owned = new EchoLensUpgrades(2, 32, 3, 2);
		ResonanceTablePlan plan = ResonanceTablePlan.between(owned, EchoLensUpgrades.DEFAULT);
		assertTrue(plan.isEmpty());
		assertEquals(owned, plan.target());
		assertEquals(0, plan.experienceLevels());
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
