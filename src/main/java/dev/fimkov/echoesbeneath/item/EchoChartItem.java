package dev.fimkov.echoesbeneath.item;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.chart.EchoChartData;
import dev.fimkov.echoesbeneath.registry.EchoComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class EchoChartItem extends BlockItem {
	public EchoChartItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
			TooltipFlag flag) {
		EchoChartData data = stack.get(EchoComponents.CHART_DATA);

		if (data == null) {
			tooltip.accept(Component.translatable(EchoTranslations.TOOLTIP_CHART_BLANK)
					.withStyle(ChatFormatting.DARK_GRAY));

			return;
		}

		tooltip.accept(Component.translatable(EchoTranslations.TOOLTIP_CHART_PLACE,
				data.center().getX(), data.center().getY(), data.center().getZ())
				.withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable(EchoTranslations.TOOLTIP_CHART_SLICE,
				data.lowestY(), data.highestY()).withStyle(ChatFormatting.DARK_AQUA));
		tooltip.accept(Component.translatable(EchoTranslations.TOOLTIP_CHART_SOUNDED,
				data.soundedBlocks()).withStyle(ChatFormatting.DARK_GRAY));
	}
}
