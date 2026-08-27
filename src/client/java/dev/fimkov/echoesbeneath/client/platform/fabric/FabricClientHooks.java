package dev.fimkov.echoesbeneath.client.platform.fabric;

import java.util.function.Supplier;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import dev.fimkov.echoesbeneath.client.platform.EchoClientHooks;
import dev.fimkov.echoesbeneath.client.platform.EchoRenderContext;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class FabricClientHooks implements EchoClientHooks {
	@Override
	public <T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type,
			Consumer<T> handler) {
		ClientPlayNetworking.registerGlobalReceiver(type,
				(payload, context) -> context.client().execute(() -> handler.accept(payload)));
	}

	@Override
	public <T extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
			BlockEntityType<T> type,
			BlockEntityRendererProvider<T, S> provider) {
		BlockEntityRendererRegistry.register(type, provider);
	}

	@Override
	public void onClientTick(Runnable handler) {
		ClientTickEvents.END_CLIENT_TICK.register(client -> handler.run());
	}

	@Override
	public void onDisconnect(Runnable handler) {
		ClientPlayConnectionEvents.DISCONNECT.register((networkHandler, client) -> handler.run());
	}

	@Override
	public <T extends Entity> void registerEntityRenderer(EntityType<T> type,
			EntityRendererProvider<T> provider) {
		EntityRendererRegistry.register(type, provider);
	}

	@Override
	public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerScreen(
			MenuType<M> type, MenuScreens.ScreenConstructor<M, S> constructor) {
		MenuScreens.register(type, constructor);
	}

	@Override
	public void registerPictureInPicture(Supplier<PictureInPictureRenderer<?>> renderer) {
		PictureInPictureRendererRegistry.register(context -> renderer.get());
	}

	@Override
	public void registerKey(KeyMapping mapping) {
		KeyMappingHelper.registerKeyMapping(mapping);
	}

	// Everything the mod draws is collected in one pass now, so the three old
	// frame hooks all hang off the submit event.
	@Override
	public void onFrameStart(Consumer<EchoRenderContext> handler) {
		LevelRenderEvents.COLLECT_SUBMITS.register(context ->
				handler.accept(new FabricRenderContext(context)));
	}

	@Override
	public void onAfterEntities(Consumer<EchoRenderContext> handler) {
		LevelRenderEvents.COLLECT_SUBMITS.register(context ->
				handler.accept(new FabricRenderContext(context)));
	}

	@Override
	public void onFrameEnd(Consumer<EchoRenderContext> handler) {
		LevelRenderEvents.COLLECT_SUBMITS.register(context ->
				handler.accept(new FabricRenderContext(context)));
	}
}
