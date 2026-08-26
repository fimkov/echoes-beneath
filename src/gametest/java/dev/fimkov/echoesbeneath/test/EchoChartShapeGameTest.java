package dev.fimkov.echoesbeneath.test;

import dev.fimkov.echoesbeneath.blockentity.EchoChartBlockEntity;
import dev.fimkov.echoesbeneath.chart.EchoChartLook;
import dev.fimkov.echoesbeneath.registry.EchoBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;

public class EchoChartShapeGameTest implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void theOutlineGrowsWithTheChosenSize(GameTestHelper helper) {
		BlockPos where = helper.absolutePos(new BlockPos(1, 2, 1));
		helper.getLevel().setBlockAndUpdate(where, EchoBlocks.ECHO_CHART.defaultBlockState());

		if (!(helper.getLevel().getBlockEntity(where) instanceof EchoChartBlockEntity chart)) {
			helper.fail("the chart block should carry a block entity");

			return;
		}

		double previous = -1.0D;

		for (int size = 0; size < EchoChartLook.SIZE_CHOICES; size++) {
			chart.setLook(EchoChartLook.DEFAULT.withSize(size));
			AABB box = EchoBlocks.ECHO_CHART.defaultBlockState()
					.getShape(helper.getLevel(), where, CollisionContext.empty())
					.toAabbs().getFirst();
			double width = box.getXsize();

			if (width <= previous) {
				helper.fail("size " + size + " is " + width + " wide, no wider than the one before");
			}
			previous = width;

			if (!EchoBlocks.ECHO_CHART.defaultBlockState()
					.getCollisionShape(helper.getLevel(), where).isEmpty()) {
				helper.fail("a chart should never block the way");
			}
		}

		if (previous < 1.0D) {
			helper.fail("the largest chart should fill its block, it is " + previous + " wide");
		}

		helper.getLevel().removeBlock(where, false);
		helper.succeed();
	}
}
