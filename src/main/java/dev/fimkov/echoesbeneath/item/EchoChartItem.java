package dev.fimkov.echoesbeneath.item;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.chart.EchoChartData;
import dev.fimkov.echoesbeneath.memory.EchoTextFormat;
import dev.fimkov.echoesbeneath.memory.storage.EchoMemoryStorage;
import dev.fimkov.echoesbeneath.record.EchoRecordData;
import dev.fimkov.echoesbeneath.registry.EchoComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class EchoChartItem extends Item {
	public EchoChartItem(Properties properties) {
		super(properties);
	}

	public static EchoRecordData recordData(ItemStack stack, EchoMemoryStorage storage) {
		EchoRecordData edit = stack.get(EchoComponents.RECORD_DATA);
		if (edit != null) {
			return edit;
		}
		EchoChartData old = stack.get(EchoComponents.CHART_DATA);
		if (old == null) {
			return null;
		}
		return storage.load(old.memoryId())
				.map(memory -> EchoRecordData.edited(memory.id(), memory.durationTicks(), 0, memory.durationTicks(), 100))
				.orElse(null);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		EchoRecordData edit = stack.get(EchoComponents.RECORD_DATA);
		if (edit == null && !stack.has(EchoComponents.CHART_DATA)) {
			tooltip.add(Component.translatable(EchoTranslations.TOOLTIP_CHART_BLANK).withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		if (edit != null) {
			tooltip.add(Component.translatable(EchoTranslations.TOOLTIP_DURATION,
					EchoTextFormat.clock(edit.durationTicks())).withStyle(ChatFormatting.AQUA));
			tooltip.add(Component.translatable("record.echoes_beneath.volume", edit.volume())
					.withStyle(ChatFormatting.GRAY));
		}
		tooltip.add(Component.translatable("record.echoes_beneath.use").withStyle(ChatFormatting.DARK_AQUA));
	}
}
