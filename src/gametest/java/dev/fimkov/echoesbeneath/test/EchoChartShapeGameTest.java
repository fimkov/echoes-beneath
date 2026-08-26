package dev.fimkov.echoesbeneath.test;

import dev.fimkov.echoesbeneath.registry.EchoBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class EchoChartShapeGameTest implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void aChartIsSmallerThanTheBlockItSitsIn(GameTestHelper helper) {
		BlockPos where = helper.absolutePos(new BlockPos(1, 2, 1));
		VoxelShape shape = EchoBlocks.ECHO_CHART.defaultBlockState()
				.getShape(helper.getLevel(), where, CollisionContext.empty());
		List<AABB> boxes = shape.toAabbs();
		if (boxes.size() != 1) {
			helper.fail("a chart should be outlined by one box, found " + boxes.size());
			return;
		}
		AABB box = boxes.getFirst();
		if (box.minX <= 0.0D || box.maxX >= 1.0D || box.minZ <= 0.0D || box.maxZ >= 1.0D) {
			helper.fail("the outline fills the whole block: " + box);
		}
		if (!shape.isEmpty() && !EchoBlocks.ECHO_CHART.defaultBlockState()
				.getCollisionShape(helper.getLevel(), where).isEmpty()) {
			helper.fail("a chart should not block the way");
		}
		helper.succeed();
	}
}
