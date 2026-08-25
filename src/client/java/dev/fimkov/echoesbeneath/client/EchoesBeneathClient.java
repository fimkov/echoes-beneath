package dev.fimkov.echoesbeneath.client;

import dev.fimkov.echoesbeneath.client.playback.EchoPlaybackReceiver;
import dev.fimkov.echoesbeneath.client.render.EchoLensEntityRenderer;
import dev.fimkov.echoesbeneath.client.render.EchoPlaybackRenderer;
import dev.fimkov.echoesbeneath.item.EchoLensTooltipContext;
import dev.fimkov.echoesbeneath.registry.EchoEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.Optional;

public final class EchoesBeneathClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EchoLensTooltipContext.bindViewer(EchoesBeneathClient::localViewer);
		EntityRendererRegistry.register(EchoEntities.ECHO_LENS, EchoLensEntityRenderer::new);
		EchoPlaybackReceiver.register();
		EchoPlaybackRenderer.register();
	}

	private static Optional<EchoLensTooltipContext.TooltipViewer> localViewer() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return Optional.empty();
		}
		return Optional.of(new EchoLensTooltipContext.TooltipViewer(player.level().dimension(), player.blockPosition()));
	}
}
