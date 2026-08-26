package dev.fimkov.echoesbeneath.test;

import dev.fimkov.echoesbeneath.chart.EchoChartData;
import dev.fimkov.echoesbeneath.menu.SoundingTableMenu;
import dev.fimkov.echoesbeneath.registry.EchoComponents;
import dev.fimkov.echoesbeneath.registry.EchoItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class EchoSoundingTableGameTest implements FabricGameTest {
	private static final int RADIUS = 32;
	private static final int LOWEST = 40;
	private static final int HIGHEST = 90;
	private static final int SOUNDED = 128;

	private static ItemStack chart() {
		ItemStack stack = new ItemStack(EchoItems.ECHO_CHART);
		stack.set(EchoComponents.CHART_DATA, new EchoChartData(UUID.randomUUID(),
				ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld")),
				BlockPos.ZERO, RADIUS, LOWEST, HIGHEST, SOUNDED));
		return stack;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aChartSurvivesTheLensArrivingAfterIt(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		SoundingTableMenu menu = new SoundingTableMenu(1, player.getInventory(),
				ContainerLevelAccess.NULL);
		menu.slots.get(SoundingTableMenu.RESULT_SLOT).set(chart());
		menu.slots.get(SoundingTableMenu.LENS_SLOT).set(new ItemStack(EchoItems.ECHO_LENS));
		if (menu.slots.get(SoundingTableMenu.RESULT_SLOT).getItem().isEmpty()) {
			helper.fail("the table ate a chart the player had put down");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void shiftClickSendsEachItemToItsOwnSlot(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		player.getInventory().add(chart());
		player.getInventory().add(new ItemStack(EchoItems.ECHO_LENS));
		SoundingTableMenu menu = new SoundingTableMenu(1, player.getInventory(),
				ContainerLevelAccess.NULL);
		for (int slot = 2; slot < menu.slots.size(); slot++) {
			if (!menu.slots.get(slot).getItem().isEmpty()) {
				menu.quickMoveStack(player, slot);
			}
		}
		if (menu.slots.get(SoundingTableMenu.LENS_SLOT).getItem().isEmpty()) {
			helper.fail("a shift clicked lens should land in the lens slot");
		}
		if (menu.slots.get(SoundingTableMenu.RESULT_SLOT).getItem().isEmpty()) {
			helper.fail("a shift clicked chart should land in the chart slot");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theResultSlotTakesChartsAndNothingElse(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		SoundingTableMenu menu = new SoundingTableMenu(1, player.getInventory(),
				ContainerLevelAccess.NULL);
		if (!menu.slots.get(SoundingTableMenu.RESULT_SLOT).mayPlace(chart())) {
			helper.fail("a chart should go back into the table");
		}
		if (menu.slots.get(SoundingTableMenu.RESULT_SLOT).mayPlace(new ItemStack(EchoItems.ECHO_CHART))) {
			helper.fail("a blank chart holds nothing and should be refused");
		}
		if (menu.slots.get(SoundingTableMenu.RESULT_SLOT).mayPlace(new ItemStack(EchoItems.ECHO_LENS))) {
			helper.fail("the result slot is not for lenses");
		}
		helper.succeed();
	}
}
