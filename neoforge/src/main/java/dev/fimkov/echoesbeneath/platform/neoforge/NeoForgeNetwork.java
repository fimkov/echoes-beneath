package dev.fimkov.echoesbeneath.platform.neoforge;

import dev.fimkov.echoesbeneath.platform.EchoNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class NeoForgeNetwork implements EchoNetwork {
	private record Declaration<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
	}

	private record Inbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
			java.util.function.BiConsumer<ServerPlayer, T> handler) {
	}

	private final List<Declaration<?>> declared = new ArrayList<>();
	private final List<Inbound<?>> inbound = new ArrayList<>();
	private final Map<CustomPacketPayload.Type<?>, Consumer<CustomPacketPayload>> handlers =
			new ConcurrentHashMap<>();

	@Override
	public <T extends CustomPacketPayload> void serverToClient(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
		declared.add(new Declaration<>(type, codec));
	}

	@Override
	public <T extends CustomPacketPayload> void clientToServer(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
			java.util.function.BiConsumer<ServerPlayer, T> handler) {
		inbound.add(new Inbound<>(type, codec, handler));
	}

	@Override
	public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
		PacketDistributor.sendToPlayer(player, payload);
	}

	@SuppressWarnings("unchecked")
	public <T extends CustomPacketPayload> void bind(CustomPacketPayload.Type<T> type,
			Consumer<T> handler) {
		handlers.put(type, payload -> handler.accept((T) payload));
	}

	public void publish(PayloadRegistrar registrar) {
		for (Declaration<?> declaration : declared) {
			register(registrar, declaration);
		}
		for (Inbound<?> declaration : inbound) {
			registerInbound(registrar, declaration);
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends CustomPacketPayload> void registerInbound(PayloadRegistrar registrar,
			Inbound<?> declaration) {
		Inbound<T> typed = (Inbound<T>) declaration;
		registrar.playToServer(typed.type(), typed.codec(), (payload, context) ->
				context.enqueueWork(() -> {
					if (context.player() instanceof ServerPlayer player) {
						typed.handler().accept(player, payload);
					}
				}));
	}

	@SuppressWarnings("unchecked")
	private <T extends CustomPacketPayload> void register(PayloadRegistrar registrar,
			Declaration<?> declaration) {
		Declaration<T> typed = (Declaration<T>) declaration;
		registrar.playToClient(typed.type(), typed.codec(), (payload, context) ->
				context.enqueueWork(() -> dispatch(payload)));
	}

	private void dispatch(CustomPacketPayload payload) {
		Consumer<CustomPacketPayload> handler = handlers.get(payload.type());
		if (handler != null) {
			handler.accept(payload);
		}
	}
}
