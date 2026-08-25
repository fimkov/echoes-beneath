package dev.fimkov.echoesbeneath.item;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReferences;
import dev.fimkov.echoesbeneath.memory.EchoFormat;
import dev.fimkov.echoesbeneath.memory.EchoTextFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;

import java.util.List;
import java.util.Optional;

public class EchoLensItem extends Item {
	public EchoLensItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		return EchoLensInteraction.useOn(context);
	}

	@Override
	public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level,
			net.minecraft.world.entity.Entity holder, int slotId, boolean selected) {
		EchoLensResonance.tick(stack, level, holder, selected);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return !EchoLensStacks.references(stack).isEmpty();
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		EchoMemoryReferences references = EchoLensStacks.references(stack);
		tooltip.add(Component.translatable(EchoTranslations.TOOLTIP_MEMORIES,
				references.filledSlots(), EchoFormat.MEMORY_SLOT_COUNT).withStyle(ChatFormatting.AQUA));
		appendCapacityLine(references, tooltip);
		appendResonanceLines(references, tooltip);
	}

	private void appendCapacityLine(EchoMemoryReferences references, List<Component> tooltip) {
		if (references.isEmpty()) {
			tooltip.add(Component.translatable(EchoTranslations.TOOLTIP_SILENT).withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		Optional<Integer> nextSlot = references.nextFreeSlot();
		if (nextSlot.isPresent()) {
			tooltip.add(Component.translatable(EchoTranslations.TOOLTIP_WILL_RECORD,
					EchoTextFormat.romanSlot(nextSlot.get())).withStyle(ChatFormatting.DARK_AQUA));
			return;
		}
		tooltip.add(Component.translatable(EchoTranslations.TOOLTIP_FULL).withStyle(ChatFormatting.DARK_GRAY));
	}

	private void appendResonanceLines(EchoMemoryReferences references, List<Component> tooltip) {
		Optional<EchoMemoryReference> resonating = EchoLensTooltipContext.resonatingHere(references);
		if (resonating.isEmpty()) {
			return;
		}
		EchoMemoryReference reference = resonating.get();
		tooltip.add(Component.translatable(EchoTranslations.TOOLTIP_RESONATES,
				EchoTextFormat.romanSlot(reference.slot())).withStyle(ChatFormatting.LIGHT_PURPLE));
		tooltip.add(Component.translatable(EchoTranslations.TOOLTIP_DURATION,
				EchoTextFormat.clock(reference.durationTicks())).withStyle(ChatFormatting.GRAY));
	}
}
