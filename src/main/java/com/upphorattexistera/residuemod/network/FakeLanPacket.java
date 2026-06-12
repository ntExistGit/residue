package com.upphorattexistera.residuemod.network;

import com.upphorattexistera.residuemod.Residue;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class FakeLanPacket {

    public static final CustomPayload.Id<Payload> ID =
            new CustomPayload.Id<>(Identifier.of("residue", "fake_lan"));

    public static final PacketCodec<RegistryByteBuf, Payload> CODEC =
            PacketCodec.unit(new Payload());

    public record Payload() implements CustomPayload {
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(ID, CODEC);
    }

    public static void sendTrigger(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, new Payload());
        }
        Residue.LOGGER.info("[Residue] FakeLan event triggered");
    }
}