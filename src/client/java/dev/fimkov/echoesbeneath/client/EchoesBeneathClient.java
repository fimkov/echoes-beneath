package dev.fimkov.echoesbeneath.client;

import dev.fimkov.echoesbeneath.client.playback.EchoPlaybackReceiver;
import dev.fimkov.echoesbeneath.client.render.EchoLensEntityRenderer;
import dev.fimkov.echoesbeneath.client.render.EchoPlaybackRenderer;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.item.EchoLensAppearance;
import dev.fimkov.echoesbeneath.item.EchoLensStacks;
import dev.fimkov.echoesbeneath.item.EchoLensTooltipContext;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.registry.EchoEntities;
import dev.fimkov.echoesbeneath.registry.EchoItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;

import java.util.Optional;

public final class EchoesBeneathClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EchoLensTooltipContext.bindViewer(EchoesBeneathClient::localViewer);
		EntityRendererRegistry.register(EchoEntities.ECHO_LENS, EchoLensEntityRenderer::new);
		EchoPlaybackReceiver.register();
		EchoPlaybackRenderer.register();
		registerLensAppearance();
		net.minecraft.client.gui.screens.MenuScreens.register(
				dev.fimkov.echoesbeneath.registry.EchoMenus.RESONANCE_TABLE,
				dev.fimkov.echoesbeneath.client.screen.ResonanceTableScreen::new);
	}

	private static void registerLensAppearance() {
		ItemProperties.register(EchoItems.ECHO_LENS, EchoesBeneath.id("tuning"), (stack, level, holder, seed) -> {
			EchoLensUpgrades upgrades = EchoLensStacks.upgrades(stack);
			return EchoLensAppearance.tuningValue(upgrades.vision(), upgrades.memorySlots(),
					EchoLensStacks.references(stack).filledSlots());
		});
	}

	private static Optional<EchoLensTooltipContext.TooltipViewer> localViewer() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return Optional.empty();
		}
		return Optional.of(new EchoLensTooltipContext.TooltipViewer(player.level().dimension(), player.blockPosition()));
	}
}
