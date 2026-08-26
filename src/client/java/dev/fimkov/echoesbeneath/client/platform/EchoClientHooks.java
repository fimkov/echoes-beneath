package dev.fimkov.echoesbeneath.client.platform;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Consumer;

public interface EchoClientHooks {
	<T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type, Consumer<T> handler);

	<T extends net.minecraft.world.level.block.entity.BlockEntity> void registerBlockEntityRenderer(
			net.minecraft.world.level.block.entity.BlockEntityType<T> type,
			net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<T> provider);

	void onClientTick(Runnable handler);

	void onDisconnect(Runnable handler);

	<T extends Entity> void registerEntityRenderer(EntityType<T> type,
			EntityRendererProvider<T> provider);

	<M extends AbstractContainerMenu, S extends net.minecraft.client.gui.screens.Screen
			& net.minecraft.client.gui.screens.inventory.MenuAccess<M>> void registerScreen(
			MenuType<M> type, MenuScreens.ScreenConstructor<M, S> constructor);

	void registerKey(net.minecraft.client.KeyMapping mapping);

	void onFrameStart(Consumer<EchoRenderContext> handler);

	void onAfterEntities(Consumer<EchoRenderContext> handler);

	void onFrameEnd(Consumer<EchoRenderContext> handler);
}
