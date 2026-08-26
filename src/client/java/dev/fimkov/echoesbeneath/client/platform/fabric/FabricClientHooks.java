package dev.fimkov.echoesbeneath.client.platform.fabric;

import dev.fimkov.echoesbeneath.client.platform.EchoClientHooks;
import dev.fimkov.echoesbeneath.client.platform.EchoRenderContext;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Consumer;

public final class FabricClientHooks implements EchoClientHooks {
	@Override
	public <T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type,
			Consumer<T> handler) {
		ClientPlayNetworking.registerGlobalReceiver(type,
				(payload, context) -> context.client().execute(() -> handler.accept(payload)));
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
	public void onFrameStart(Consumer<EchoRenderContext> handler) {
		WorldRenderEvents.START.register(context -> handler.accept(new FabricRenderContext(context)));
	}

	@Override
	public void onAfterEntities(Consumer<EchoRenderContext> handler) {
		WorldRenderEvents.AFTER_ENTITIES.register(context ->
				handler.accept(new FabricRenderContext(context)));
	}

	@Override
	public void onFrameEnd(Consumer<EchoRenderContext> handler) {
		WorldRenderEvents.END.register(context -> handler.accept(new FabricRenderContext(context)));
	}
}
