package dev.fimkov.echoesbeneath.test;

import dev.fimkov.echoesbeneath.blockentity.EchoChartBlockEntity;
import dev.fimkov.echoesbeneath.chart.EchoChartData;
import dev.fimkov.echoesbeneath.chart.EchoChartLook;
import dev.fimkov.echoesbeneath.registry.EchoBlocks;
import dev.fimkov.echoesbeneath.registry.EchoComponents;
import dev.fimkov.echoesbeneath.registry.EchoItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class EchoChartBlockGameTest implements FabricGameTest {
	private static final BlockPos WHERE = new BlockPos(1, 2, 1);

	private static ItemStack chart() {
		ItemStack stack = new ItemStack(EchoItems.ECHO_CHART);
		stack.set(EchoComponents.CHART_DATA, new EchoChartData(UUID.randomUUID(),
				ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("overworld")),
				BlockPos.ZERO, 32, 40, 90, 128));
		stack.set(EchoComponents.CHART_LOOK, EchoChartLook.DEFAULT.withSpin(EchoChartLook.SPIN_BRISK));

		return stack;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void placingAChartHandsItsDataToTheBlock(GameTestHelper helper) {
		BlockPos absolute = helper.absolutePos(WHERE);
		helper.getLevel().setBlockAndUpdate(absolute, EchoBlocks.ECHO_CHART.defaultBlockState());

		if (!(helper.getLevel().getBlockEntity(absolute) instanceof EchoChartBlockEntity block)) {
			helper.fail("the chart block should carry a block entity");

			return;
		}

		EchoBlocks.ECHO_CHART.setPlacedBy(helper.getLevel(), absolute,
				EchoBlocks.ECHO_CHART.defaultBlockState(), null, chart());

		if (block.chart() == null) {
			helper.fail("the block took none of the chart's data");
		}

		if (block.look().spin() != EchoChartLook.SPIN_BRISK) {
			helper.fail("the block took none of the chart's display settings");
		}

		ItemStack back = block.asItem();

		if (back.get(EchoComponents.CHART_DATA) == null) {
			helper.fail("breaking the block would hand back a blank chart");
		}

		if (back.get(EchoComponents.CHART_LOOK).spin() != EchoChartLook.SPIN_BRISK) {
			helper.fail("breaking the block would lose its display settings");
		}

		helper.getLevel().removeBlock(absolute, false);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aChartBlockGivesOffLight(GameTestHelper helper) {
		int emission = EchoBlocks.ECHO_CHART.defaultBlockState().getLightEmission();

		if (emission <= 0) {
			helper.fail("a chart should light its own corner, emission is " + emission);
		}

		helper.succeed();
	}
}
