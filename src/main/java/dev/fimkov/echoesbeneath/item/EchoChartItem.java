package dev.fimkov.echoesbeneath.item;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.chart.EchoChartData;
import dev.fimkov.echoesbeneath.entity.EchoChartEntity;
import dev.fimkov.echoesbeneath.registry.EchoComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class EchoChartItem extends Item {
	public static final double HOVER_HEIGHT = 0.0D;

	public EchoChartItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		ItemStack stack = context.getItemInHand();
		if (stack.get(EchoComponents.CHART_DATA) == null) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockPos above = context.getClickedPos().relative(context.getClickedFace());
		EchoChartEntity chart = new EchoChartEntity(level, stack);
		chart.setPos(above.getX() + 0.5D, above.getY() + HOVER_HEIGHT, above.getZ() + 0.5D);
		if (!level.addFreshEntity(chart)) {
			return InteractionResult.PASS;
		}
		if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
			stack.shrink(1);
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
			TooltipFlag flag) {
		EchoChartData data = stack.get(EchoComponents.CHART_DATA);
		if (data == null) {
			tooltip.add(Component.translatable(EchoTranslations.TOOLTIP_CHART_BLANK)
					.withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		tooltip.add(Component.translatable(EchoTranslations.TOOLTIP_CHART_PLACE,
				data.center().getX(), data.center().getY(), data.center().getZ())
				.withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable(EchoTranslations.TOOLTIP_CHART_SLICE,
				data.lowestY(), data.highestY()).withStyle(ChatFormatting.DARK_AQUA));
		tooltip.add(Component.translatable(EchoTranslations.TOOLTIP_CHART_SOUNDED,
				data.soundedBlocks()).withStyle(ChatFormatting.DARK_GRAY));
	}
}
