package dev.fimkov.echoesbeneath.client;

import dev.fimkov.echoesbeneath.client.playback.EchoPlaybackReceiver;
import dev.fimkov.echoesbeneath.client.render.EchoLensEntityRenderer;
import dev.fimkov.echoesbeneath.client.platform.EchoClientPlatforms;
import dev.fimkov.echoesbeneath.client.platform.fabric.FabricClientHooks;
import dev.fimkov.echoesbeneath.client.render.EchoPlaybackRenderer;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.client.config.EchoClientConfig;
import dev.fimkov.echoesbeneath.item.EchoLensAppearance;
import dev.fimkov.echoesbeneath.item.EchoLensStacks;
import dev.fimkov.echoesbeneath.item.EchoLensTooltipContext;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.registry.EchoEntities;
import dev.fimkov.echoesbeneath.registry.EchoItems;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;

import java.util.Optional;

public final class EchoesBeneathClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EchoClientPlatforms.install(new FabricClientHooks());
		EchoClientConfig.load();
		EchoLensTooltipContext.bindViewer(EchoesBeneathClient::localViewer);
		EchoClientPlatforms.get().registerEntityRenderer(EchoEntities.ECHO_LENS,
				EchoLensEntityRenderer::new);
		EchoClientPlatforms.get().registerEntityRenderer(EchoEntities.ECHO_CHART,
				dev.fimkov.echoesbeneath.client.render.EchoChartRenderer::new);
		EchoClientPlatforms.get().registerKey(
				dev.fimkov.echoesbeneath.client.input.EchoKeys.OPEN_CONFIG);
		EchoPlaybackReceiver.register();
		EchoClientPlatforms.get().onFrameStart(EchoPlaybackRenderer::frameStart);
		EchoClientPlatforms.get().onAfterEntities(EchoPlaybackRenderer::afterEntities);
		EchoClientPlatforms.get().onFrameEnd(EchoPlaybackRenderer::frameEnd);
		registerLensAppearance();
		EchoClientPlatforms.get().registerScreen(
				dev.fimkov.echoesbeneath.registry.EchoMenus.RESONANCE_TABLE,
				dev.fimkov.echoesbeneath.client.screen.ResonanceTableScreen::new);
	}

	private static void registerLensAppearance() {
		ItemProperties.register(EchoItems.ECHO_LENS, EchoesBeneath.id("tuning"), (stack, level, holder, seed) -> {
			EchoLensUpgrades upgrades = EchoLensStacks.upgrades(stack);
			return EchoLensAppearance.tuningValue(upgrades.vision(), upgrades.memorySlots(),
					EchoLensStacks.references(stack).filledSlots(), upgrades.radius(),
					upgrades.sensitivity());
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
