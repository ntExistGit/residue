package com.upphorattexistera.residue.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ObserverMessageRequestPacket {

    public static final CustomPayload.Id<Payload> ID =
            new CustomPayload.Id<>(Identifier.of("residue", "observer_message_request"));

    public record Payload(String observerName, String playerMessage)
            implements CustomPayload {

        public static final PacketCodec<RegistryByteBuf, Payload> CODEC =
                PacketCodec.of(
                        (value, buf) -> {
                            buf.writeString(value.observerName());
                            buf.writeString(value.playerMessage());
                        },
                        buf -> new Payload(buf.readString(), buf.readString())
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void register() {
        // Регистрируем серверный обработчик
        PayloadTypeRegistry.serverboundPlay().register(ID, Payload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            String observerName = payload.observerName();
            String playerMessage = payload.playerMessage();

            // Проверяем что обсервер действительно активен
            boolean isActive = com.upphorattexistera.residue.observer.ObserverSessionManager
                    .getSessions().stream()
                    .anyMatch(s -> s.observer.getName().equalsIgnoreCase(observerName));

            if (!isActive) return;

            // Пересылаем клиенту для обработки LLM
            ObserverMessagePacket.sendToPlayer(context.player(), observerName, playerMessage);
        });
    }
}