package dev.fimkov.echoesbeneath.item;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.chart.EchoChartData;
import dev.fimkov.echoesbeneath.memory.EchoTextFormat;
import dev.fimkov.echoesbeneath.memory.storage.EchoMemoryStorage;
import dev.fimkov.echoesbeneath.record.EchoRecordData;
import dev.fimkov.echoesbeneath.registry.EchoComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

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
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		EchoRecordData edit = stack.get(EchoComponents.RECORD_DATA);
		if (edit == null && !stack.has(EchoComponents.CHART_DATA)) {
			tooltip.accept(Component.translatable(EchoTranslations.TOOLTIP_CHART_BLANK).withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		if (edit != null) {
			tooltip.accept(Component.translatable(EchoTranslations.TOOLTIP_DURATION,
					EchoTextFormat.clock(edit.durationTicks())).withStyle(ChatFormatting.AQUA));
			tooltip.accept(Component.translatable("record.echoes_beneath.volume", edit.volume())
					.withStyle(ChatFormatting.GRAY));
		}
		tooltip.accept(Component.translatable("record.echoes_beneath.use").withStyle(ChatFormatting.DARK_AQUA));
	}
}
