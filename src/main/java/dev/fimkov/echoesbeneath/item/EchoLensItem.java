package dev.fimkov.echoesbeneath.item;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReferences;
import dev.fimkov.echoesbeneath.memory.EchoTextFormat;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class EchoLensItem extends Item {
	public EchoLensItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		return EchoLensInteraction.useOn(context);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level,
			Entity holder, EquipmentSlot slot) {
		EchoLensResonance.tick(stack, level, holder, slot == EquipmentSlot.MAINHAND);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		EchoMemoryReferences references = EchoLensStacks.references(stack);
		EchoLensUpgrades upgrades = EchoLensStacks.upgrades(stack);
		tooltip.accept(Component.translatable(EchoTranslations.TOOLTIP_MEMORIES,
				references.filledSlots(), upgrades.memorySlots()).withStyle(ChatFormatting.AQUA));
		appendCapacityLine(references, upgrades, tooltip);
		tooltip.accept(Component.translatable(EchoTranslations.TOOLTIP_TUNING,
				upgrades.radius(), upgrades.sensitivity(), upgrades.vision()).withStyle(ChatFormatting.DARK_GRAY));
		appendResonanceLines(references, tooltip);
	}

	private void appendCapacityLine(EchoMemoryReferences references, EchoLensUpgrades upgrades,
			Consumer<Component> tooltip) {
		if (references.isEmpty()) {
			tooltip.accept(Component.translatable(EchoTranslations.TOOLTIP_SILENT).withStyle(ChatFormatting.DARK_GRAY));

			return;
		}

		Optional<Integer> nextSlot = references.nextFreeSlot(upgrades.memorySlots());

		if (nextSlot.isPresent()) {
			tooltip.accept(Component.translatable(EchoTranslations.TOOLTIP_WILL_RECORD,
					EchoTextFormat.romanSlot(nextSlot.get())).withStyle(ChatFormatting.DARK_AQUA));

			return;
		}

		tooltip.accept(Component.translatable(EchoTranslations.TOOLTIP_FULL).withStyle(ChatFormatting.DARK_GRAY));
	}

	private void appendResonanceLines(EchoMemoryReferences references, Consumer<Component> tooltip) {
		Optional<EchoMemoryReference> resonating = EchoLensTooltipContext.resonatingHere(references);

		if (resonating.isEmpty()) {
			return;
		}

		EchoMemoryReference reference = resonating.get();
		tooltip.accept(Component.translatable(EchoTranslations.TOOLTIP_RESONATES,
				EchoTextFormat.romanSlot(reference.slot())).withStyle(ChatFormatting.LIGHT_PURPLE));
		tooltip.accept(Component.translatable(EchoTranslations.TOOLTIP_DURATION,
				EchoTextFormat.clock(reference.durationTicks())).withStyle(ChatFormatting.GRAY));
	}
}
