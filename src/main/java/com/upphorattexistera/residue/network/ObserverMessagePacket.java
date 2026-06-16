package com.upphorattexistera.residue.network;

import com.upphorattexistera.residue.Residue;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ObserverMessagePacket {

    public static final CustomPayload.Id<Payload> ID =
            new CustomPayload.Id<>(Identifier.of("residue", "observer_message"));

    public record Payload(
            String observerName,
            String playerMessage,
            String systemPrompt,
            double temperature,
            int maxTokens,
            String historyJson
    ) implements CustomPayload {

        public static final PacketCodec<RegistryByteBuf, Payload> CODEC =
                PacketCodec.of(
                        (value, buf) -> {
                            buf.writeString(value.observerName());
                            buf.writeString(value.playerMessage());
                            buf.writeString(value.systemPrompt());
                            buf.writeDouble(value.temperature());
                            buf.writeInt(value.maxTokens());
                            buf.writeString(value.historyJson());
                        },
                        buf -> new Payload(
                                buf.readString(),
                                buf.readString(),
                                buf.readString(),
                                buf.readDouble(),
                                buf.readInt(),
                                buf.readString()
                        )
                );

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(ID, Payload.CODEC);
    }

    public static void sendToPlayer(ServerPlayerEntity player, Payload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}