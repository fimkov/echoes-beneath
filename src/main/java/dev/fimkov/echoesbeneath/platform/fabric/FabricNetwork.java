package dev.fimkov.echoesbeneath.platform.fabric;

import dev.fimkov.echoesbeneath.platform.EchoNetwork;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class FabricNetwork implements EchoNetwork {
	@Override
	public <T extends CustomPacketPayload> void serverToClient(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
		PayloadTypeRegistry.playS2C().register(type, cast(codec));
	}

	@Override
	public <T extends CustomPacketPayload> void clientToServer(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
			java.util.function.BiConsumer<ServerPlayer, T> handler) {
		PayloadTypeRegistry.playC2S().register(type, cast(codec));
		ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
				context.player().server.execute(() -> handler.accept(context.player(), payload)));
	}

	@Override
	public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
		ServerPlayNetworking.send(player, payload);
	}

	@SuppressWarnings("unchecked")
	private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> cast(
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
		return (StreamCodec<RegistryFriendlyByteBuf, T>) codec;
	}
}
