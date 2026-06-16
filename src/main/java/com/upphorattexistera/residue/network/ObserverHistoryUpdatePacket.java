package com.upphorattexistera.residue.network;

import com.upphorattexistera.residue.observer.persona.ObserverDataStore;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ObserverHistoryUpdatePacket {

    public static final CustomPayload.Id<Payload> ID =
            new CustomPayload.Id<>(Identifier.of("residue", "observer_history_update"));

    public record Payload(String observerName, String userMessage, String assistantReply)
            implements CustomPayload {

        public static final PacketCodec<RegistryByteBuf, Payload> CODEC =
                PacketCodec.of(
                        (value, buf) -> {
                            buf.writeString(value.observerName());
                            buf.writeString(value.userMessage());
                            buf.writeString(value.assistantReply());
                        },
                        buf -> new Payload(
                                buf.readString(),
                                buf.readString(),
                                buf.readString()
                        )
                );

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(ID, Payload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            ObserverDataStore.addToHistory(
                    payload.observerName(), "user", payload.userMessage());
            ObserverDataStore.addToHistory(
                    payload.observerName(), "assistant", payload.assistantReply());
            ObserverDataStore.save();
        });
    }
}