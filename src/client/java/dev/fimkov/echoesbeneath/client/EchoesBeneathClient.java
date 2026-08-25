package dev.fimkov.echoesbeneath.client;

import dev.fimkov.echoesbeneath.item.EchoLensTooltipContext;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.Optional;

public final class EchoesBeneathClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EchoLensTooltipContext.bindViewer(EchoesBeneathClient::localViewer);
	}

	private static Optional<EchoLensTooltipContext.TooltipViewer> localViewer() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return Optional.empty();
		}
		return Optional.of(new EchoLensTooltipContext.TooltipViewer(player.level().dimension(), player.blockPosition()));
	}
}
