package dev.fimkov.echoesbeneath.client;

import dev.fimkov.echoesbeneath.client.config.EchoClientConfig;
import dev.fimkov.echoesbeneath.client.input.EchoKeys;
import dev.fimkov.echoesbeneath.client.platform.EchoClientPlatforms;
import dev.fimkov.echoesbeneath.client.platform.fabric.FabricClientHooks;
import dev.fimkov.echoesbeneath.client.playback.EchoPlaybackReceiver;
import dev.fimkov.echoesbeneath.client.render.EchoChartBlockRenderer;
import dev.fimkov.echoesbeneath.client.render.EchoLensEntityRenderer;
import dev.fimkov.echoesbeneath.client.render.EchoPlaybackRenderer;
import dev.fimkov.echoesbeneath.client.screen.ChartLookScreen;
import dev.fimkov.echoesbeneath.client.screen.ResonanceTableScreen;
import dev.fimkov.echoesbeneath.client.screen.SoundingTableScreen;
import dev.fimkov.echoesbeneath.item.EchoLensTooltipContext;
import dev.fimkov.echoesbeneath.registry.EchoBlockEntities;
import dev.fimkov.echoesbeneath.registry.EchoEntities;
import dev.fimkov.echoesbeneath.registry.EchoMenus;
import java.util.Optional;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class EchoesBeneathClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EchoClientPlatforms.install(new FabricClientHooks());
		EchoClientConfig.load();
		EchoLensTooltipContext.bindViewer(EchoesBeneathClient::localViewer);
		EchoClientPlatforms.get().registerEntityRenderer(EchoEntities.ECHO_LENS,
				EchoLensEntityRenderer::new);
		EchoClientPlatforms.get().registerBlockEntityRenderer(
				EchoBlockEntities.ECHO_CHART,
				EchoChartBlockRenderer::new);
		EchoClientPlatforms.get().registerKey(
				EchoKeys.OPEN_CONFIG);
		EchoPlaybackReceiver.register();
		EchoClientPlatforms.get().onFrameStart(EchoPlaybackRenderer::frameStart);
		EchoClientPlatforms.get().onAfterEntities(EchoPlaybackRenderer::afterEntities);
		EchoClientPlatforms.get().onFrameEnd(EchoPlaybackRenderer::frameEnd);
		EchoClientPlatforms.get().registerScreen(
				EchoMenus.RESONANCE_TABLE,
				ResonanceTableScreen::new);
		EchoClientPlatforms.get().registerScreen(
				EchoMenus.SOUNDING_TABLE,
				SoundingTableScreen::new);
		EchoClientPlatforms.get().registerScreen(
				EchoMenus.CHART_LOOK,
				ChartLookScreen::new);
	}

	private static Optional<EchoLensTooltipContext.TooltipViewer> localViewer() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return Optional.empty();
		}
		return Optional.of(new EchoLensTooltipContext.TooltipViewer(player.level().dimension(), player.blockPosition()));
	}
}
