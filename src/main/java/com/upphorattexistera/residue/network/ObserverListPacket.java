package com.upphorattexistera.residue.network;

import com.upphorattexistera.residue.Residue;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ObserverListPacket {

    public static final CustomPayload.Id<Payload> ID =
            new CustomPayload.Id<>(Identifier.of("residue", "observer_list"));

    public record ObserverEntry(UUID uuid, String name, int latency,
                                @Nullable String skinTextureId, boolean slim,
                                String ttsSpeaker) {}

    public record Payload(List<ObserverEntry> observers) implements CustomPayload {

        public static final PacketCodec<RegistryByteBuf, Payload> CODEC =
                PacketCodec.of(
                        (value, buf) -> {
                            buf.writeVarInt(value.observers().size());
                            for (ObserverEntry e : value.observers()) {
                                buf.writeUuid(e.uuid());
                                buf.writeString(e.name());
                                buf.writeVarInt(e.latency());
                                boolean hasSkin = e.skinTextureId() != null;
                                buf.writeBoolean(hasSkin);
                                if (hasSkin) buf.writeString(e.skinTextureId());
                                buf.writeBoolean(e.slim());
                                buf.writeString(e.ttsSpeaker() != null ? e.ttsSpeaker() : "");
                            }
                        },
                        buf -> {
                            int size = buf.readVarInt();
                            List<ObserverEntry> list = new ArrayList<>(size);
                            for (int i = 0; i < size; i++) {
                                UUID uuid = buf.readUuid();
                                String name = buf.readString();
                                int latency = buf.readVarInt();
                                boolean hasSkin = buf.readBoolean();
                                String skinId = hasSkin ? buf.readString() : null;
                                boolean slim = buf.readBoolean();
                                String ttsSpeaker = buf.readString();
                                list.add(new ObserverEntry(uuid, name, latency, skinId, slim, ttsSpeaker));
                            }
                            return new Payload(list);
                        }
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(ID, Payload.CODEC);
    }

    public static void sendToAll(MinecraftServer server, List<ObserverEntry> observers) {
        Payload payload = new Payload(observers);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
        Residue.LOGGER.debug("[residue] ObserverList sent: {} observers", observers.size());
    }
}