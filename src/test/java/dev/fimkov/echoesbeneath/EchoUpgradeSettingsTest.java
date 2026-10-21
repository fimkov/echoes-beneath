package dev.fimkov.echoesbeneath;

import dev.fimkov.echoesbeneath.config.EchoConfig;
import dev.fimkov.echoesbeneath.config.EchoUpgradeSettings;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.menu.ResonanceTablePlan;
import dev.fimkov.echoesbeneath.menu.ResonanceTableUpgrade;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoUpgradeSettingsTest {
	@Test
	void modpacksCanDisableVisionOnExistingLensesAndChangePrices() {
		Properties properties = new Properties();
		properties.setProperty("upgrades.vision.maxSteps", "0");
		properties.setProperty("upgrades.radius.maxSteps", "2");
		properties.setProperty("upgrades.radius.experiencePerStep", "7");
		properties.setProperty("upgrades.radius.shardsPerStep", "0");
		EchoUpgradeSettings settings = EchoConfig.sanitize(properties).upgrades();
		EchoLensUpgrades desired = new EchoLensUpgrades(1, 64, 0, 4);
		ResonanceTablePlan plan = ResonanceTablePlan.of(EchoLensUpgrades.DEFAULT,
				EchoLensUpgrades.DEFAULT, EchoLensUpgrades.DEFAULT, desired, settings);
		assertEquals(0, settings.limit(desired).vision());
		assertEquals(0, plan.target().vision());
		assertEquals(32, plan.target().radius());
		assertEquals(14, plan.experienceLevels());
		assertEquals(0, plan.amethystShards());
	}

	@Test
	void loweringTheMemoryCapNeverRemovesSealedSlots() {
		Properties properties = new Properties();
		properties.setProperty("upgrades.memory_slot.maxSteps", "0");
		EchoLensUpgrades full = new EchoLensUpgrades(3, 16, 0, 0);
		ResonanceTablePlan plan = ResonanceTablePlan.of(full,
				ResonanceTableUpgrade.lowestSetting(3), full, full, EchoUpgradeSettings.read(properties));
		assertEquals(3, plan.target().memorySlots());
		assertFalse(plan.changed());
	}

	@Test
	void configurationRoundTripsAndRejectsUnsupportedLimits() {
		Properties properties = new Properties();
		properties.setProperty("upgrades.vision.maxSteps", "999");
		properties.setProperty("upgrades.sensitivity.maxSteps", "-1");
		properties.setProperty("upgrades.radius.experiencePerStep", "bad");
		EchoUpgradeSettings settings = EchoUpgradeSettings.read(properties);
		assertEquals(4, settings.track(ResonanceTableUpgrade.VISION).maxSteps());
		assertEquals(0, settings.track(ResonanceTableUpgrade.SENSITIVITY).maxSteps());
		assertEquals(6, settings.track(ResonanceTableUpgrade.RADIUS).experiencePerStep());
		Properties saved = new Properties();
		settings.write(saved);
		assertEquals(settings, EchoUpgradeSettings.read(saved));
		assertFalse(EchoConfig.defaults().sphericalCharts());
		properties.setProperty("sphericalCharts", "true");
		assertTrue(EchoConfig.sanitize(properties).sphericalCharts());
	}
}
