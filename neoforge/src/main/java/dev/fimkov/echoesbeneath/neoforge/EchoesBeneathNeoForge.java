package dev.fimkov.echoesbeneath.neoforge;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.EchoesBeneathBootstrap;
import dev.fimkov.echoesbeneath.client.config.EchoClientConfig;
import dev.fimkov.echoesbeneath.client.input.EchoKeys;
import dev.fimkov.echoesbeneath.client.platform.EchoClientPlatforms;
import dev.fimkov.echoesbeneath.client.playback.EchoPlaybackReceiver;
import dev.fimkov.echoesbeneath.client.render.EchoChartBlockRenderer;
import dev.fimkov.echoesbeneath.client.render.EchoLensEntityRenderer;
import dev.fimkov.echoesbeneath.client.render.EchoPlaybackRenderer;
import dev.fimkov.echoesbeneath.client.screen.ChartLookScreen;
import dev.fimkov.echoesbeneath.client.screen.EchoConfigScreen;
import dev.fimkov.echoesbeneath.client.screen.ResonanceTableScreen;
import dev.fimkov.echoesbeneath.client.screen.SoundingTableScreen;
import dev.fimkov.echoesbeneath.item.EchoLensTooltipContext;
import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import dev.fimkov.echoesbeneath.platform.neoforge.NeoForgeClientHooks;
import dev.fimkov.echoesbeneath.platform.neoforge.NeoForgePlatform;
import dev.fimkov.echoesbeneath.registry.EchoBlockEntities;
import dev.fimkov.echoesbeneath.registry.EchoEntities;
import dev.fimkov.echoesbeneath.registry.EchoMenus;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(EchoesBeneath.MOD_ID)
public final class EchoesBeneathNeoForge {
	private static final String PROTOCOL_VERSION = "1";

	private final NeoForgePlatform platform = new NeoForgePlatform();
	private boolean contentRegistered;

	public EchoesBeneathNeoForge(IEventBus modBus, ModContainer container) {
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			container.registerExtensionPoint(
					IConfigScreenFactory.class,
					(owner, parent) ->
							new EchoConfigScreen(parent));
		}

		EchoPlatforms.install(platform);

		modBus.addListener(RegisterEvent.class, event -> {
			if (contentRegistered) {
				return;
			}
			contentRegistered = true;
			EchoesBeneathBootstrap.registerContent();
		});

		EchoesBeneathBootstrap.registerBehaviour();
		modBus.addListener(RegisterPayloadHandlersEvent.class, event ->
				platform.neoNetwork().publish(event.registrar(PROTOCOL_VERSION)));
		modBus.addListener(BuildCreativeModeTabContentsEvent.class, platform.neoRegistries()::fillCreativeTab);

		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			startClient(modBus);
		}
	}

	private void startClient(IEventBus modBus) {
		NeoForgeClientHooks hooks = new NeoForgeClientHooks(platform.neoNetwork());
		EchoClientPlatforms.install(hooks);
		EchoClientConfig.load();
		EchoLensTooltipContext.bindViewer(EchoesBeneathNeoForge::localViewer);
		hooks.registerKey(EchoKeys.OPEN_CONFIG);
		EchoPlaybackReceiver.register();
		hooks.onFrameStart(EchoPlaybackRenderer::frameStart);
		hooks.onAfterEntities(EchoPlaybackRenderer::afterEntities);
		hooks.onFrameEnd(EchoPlaybackRenderer::frameEnd);

		modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
			event.registerEntityRenderer(EchoEntities.ECHO_LENS,
					EchoLensEntityRenderer::new);
			hooks.registerBlockEntityRenderer(
					EchoBlockEntities.ECHO_CHART,
					EchoChartBlockRenderer::new);
			hooks.bindBlockEntityRenderers(event);
		});

		modBus.addListener(RegisterKeyMappingsEvent.class,
				hooks::bindKeys);

		modBus.addListener(RegisterMenuScreensEvent.class, event -> {
			event.register(EchoMenus.RESONANCE_TABLE,
					ResonanceTableScreen::new);
			event.register(EchoMenus.SOUNDING_TABLE,
					SoundingTableScreen::new);
			event.register(EchoMenus.CHART_LOOK,
					ChartLookScreen::new);
		});
	}

	private static Optional<EchoLensTooltipContext.TooltipViewer> localViewer() {
		LocalPlayer player = Minecraft.getInstance().player;

		if (player == null) {
			return Optional.empty();
		}

		return Optional.of(new EchoLensTooltipContext.TooltipViewer(
				player.level().dimension(), player.blockPosition()));
	}
}
