package dev.fimkov.echoesbeneath.platform.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fimkov.echoesbeneath.client.platform.EchoClientHooks;
import dev.fimkov.echoesbeneath.client.platform.EchoRenderContext;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class NeoForgeClientHooks implements EchoClientHooks {
	private final NeoForgeNetwork network;
	private final IEventBus bus = NeoForge.EVENT_BUS;
	private final List<KeyMapping> pendingKeys =
			new ArrayList<>();
	private final List<BlockEntityBinding<?>> blockEntityRenderers =
			new ArrayList<>();

	private record BlockEntityBinding<T extends BlockEntity>(
			BlockEntityType<T> type,
			BlockEntityRendererProvider<T> provider) {
		@SuppressWarnings("unchecked")

		void bind(EntityRenderersEvent.RegisterRenderers event) {
			event.registerBlockEntityRenderer(type, provider);
		}
	}

	public void bindBlockEntityRenderers(
			EntityRenderersEvent.RegisterRenderers event) {
		for (BlockEntityBinding<?> binding : blockEntityRenderers) {
			binding.bind(event);
		}
	}

	public NeoForgeClientHooks(NeoForgeNetwork network) {
		this.network = network;
	}

	@Override
	public <T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type,
			Consumer<T> handler) {
		network.bind(type, handler);
	}

	@Override
	public <T extends BlockEntity> void registerBlockEntityRenderer(
			BlockEntityType<T> type,
			BlockEntityRendererProvider<T> provider) {
		blockEntityRenderers.add(new BlockEntityBinding<>(type, provider));
	}

	@Override
	public void onClientTick(Runnable handler) {
		bus.addListener(ClientTickEvent.Post.class,
				event -> handler.run());
	}

	@Override
	public void onDisconnect(Runnable handler) {
		bus.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> handler.run());
	}

	@Override
	public <T extends Entity> void registerEntityRenderer(EntityType<T> type,
			EntityRendererProvider<T> provider) {
		throw new UnsupportedOperationException("NeoForge binds entity renderers from its own event");
	}

	@Override
	public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerScreen(
			MenuType<M> type, MenuScreens.ScreenConstructor<M, S> constructor) {
		throw new UnsupportedOperationException("NeoForge binds screens from its own event");
	}

	@Override
	public void registerKey(KeyMapping mapping) {
		pendingKeys.add(mapping);
	}

	public void bindKeys(RegisterKeyMappingsEvent event) {
		for (KeyMapping mapping : pendingKeys) {
			event.register(mapping);
		}
	}

	@Override
	public void onFrameStart(Consumer<EchoRenderContext> handler) {
		bus.addListener(RenderLevelStageEvent.class, event -> {
			if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
				handler.accept(wrap(event));
			}
		});
	}

	@Override
	public void onAfterEntities(Consumer<EchoRenderContext> handler) {
		bus.addListener(RenderLevelStageEvent.class, event -> {
			if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
				handler.accept(wrap(event));
			}
		});
	}

	@Override
	public void onFrameEnd(Consumer<EchoRenderContext> handler) {
		bus.addListener(RenderLevelStageEvent.class, event -> {
			if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
				handler.accept(wrap(event));
			}
		});
	}

	private static EchoRenderContext wrap(RenderLevelStageEvent event) {
		return new StageContext(event.getPoseStack(), event.getPartialTick(), event.getCamera(),
				event.getFrustum());
	}

	private record StageContext(PoseStack pose, DeltaTracker tracker, Camera view, Frustum bounds)
			implements EchoRenderContext {
		@Override

		public PoseStack matrixStack() {
			return pose;
		}

		@Override

		public MultiBufferSource consumers() {
			return Minecraft.getInstance().renderBuffers().bufferSource();
		}

		@Override

		public DeltaTracker tickCounter() {
			return tracker;
		}

		@Override

		public Frustum frustum() {
			return bounds;
		}

		@Override

		public Camera camera() {
			return view;
		}

		@Override

		public ClientLevel world() {
			return Minecraft.getInstance().level;
		}
	}
}
