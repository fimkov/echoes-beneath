package dev.fimkov.echoesbeneath.platform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public interface EchoNetwork {
	<T extends CustomPacketPayload> void serverToClient(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec);

	<T extends CustomPacketPayload> void clientToServer(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
			java.util.function.BiConsumer<ServerPlayer, T> handler);

	void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
}
