package dev.fimkov.echoesbeneath.test;

import dev.fimkov.echoesbeneath.config.EchoConfig;
import dev.fimkov.echoesbeneath.playback.EchoMending;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

public class EchoMendingGameTest implements FabricGameTest {
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void plainBuildingBlocksCanBePutBack(GameTestHelper helper) {
		if (!EchoMending.isRestorable(Blocks.DEEPSLATE_BRICKS.defaultBlockState())) {
			helper.fail("a plain building block must be restorable");
		}
		if (!EchoMending.isRestorable(Blocks.OAK_STAIRS.defaultBlockState())) {
			helper.fail("a shaped block must be restorable, it is the shape that matters");
		}
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void nothingThatWouldConjureContentsComesBack(GameTestHelper helper) {
		if (EchoMending.isRestorable(Blocks.AIR.defaultBlockState())) {
			helper.fail("air is not something to restore");
		}
		if (EchoMending.isRestorable(Blocks.CHEST.defaultBlockState())) {
			helper.fail("a container would come back empty and must be refused");
		}
		if (EchoMending.isRestorable(Blocks.WATER.defaultBlockState())) {
			helper.fail("fluids must not be restorable");
		}
		if (EchoMending.isRestorable(Blocks.LAVA.defaultBlockState())) {
			helper.fail("fluids must not be restorable");
		}
		if (EchoMending.isRestorable(Blocks.BUBBLE_COLUMN.defaultBlockState())) {
			helper.fail("a block with no item must be refused");
		}
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void mendingIsOnAndCostsSomething(GameTestHelper helper) {
		if (!EchoConfig.defaults().allowMemoryMending()) {
			helper.fail("mending must ship enabled");
		}
		if (EchoConfig.defaults().mendingExperienceCost() <= 0) {
			helper.fail("mending must cost experience by default");
		}
		helper.succeed();
	}
}
