package com.upphorattexistera.residuemod.observer;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.upphorattexistera.residuemod.Residue;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class ObserverTabListManager {

    public static UUID uuidFromName(String name) {
        return UUID.nameUUIDFromBytes(
                ("ResidueObserver:" + name).getBytes(StandardCharsets.UTF_8)
        );
    }

    public static void sendAdd(MinecraftServer server, Observer observer) {
        PlayerListS2CPacket packet = buildAddPacket(server, observer);
        if (packet == null) return;
        broadcast(server, packet);
    }

    public static void sendRemove(MinecraftServer server, Observer observer) {
        UUID uuid = uuidFromName(observer.getName());
        PlayerRemoveS2CPacket packet = new PlayerRemoveS2CPacket(List.of(uuid));
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(packet);
        }
    }

    public static void sendAllActive(ServerPlayerEntity player) {
        MinecraftServer server = player.getEntityWorld().getServer();
        for (ObserverSessionManager.Session session : ObserverSessionManager.getSessions()) {
            PlayerListS2CPacket packet = buildAddPacket(server, session.observer);
            if (packet != null) {
                player.networkHandler.sendPacket(packet);
            }
        }
    }

    private static PlayerListS2CPacket buildAddPacket(MinecraftServer server, Observer observer) {

        try {
            UUID uuid = uuidFromName(observer.getName());

            // строим GameProfile — добавляем скин если уже нашли
            SkinData skinData = ObserverSkinResolver.getCached(observer.getName());

            PropertyMap propertyMap;
            if (skinData.hasTextures()) {
                ImmutableMultimap.Builder<String, Property> builder =
                        ImmutableMultimap.builder();
                builder.put("textures", skinData.getTexturesProperty());
                propertyMap = new PropertyMap(builder.build());
            } else {
                propertyMap = PropertyMap.EMPTY;
            }

            GameProfile profile = new GameProfile(uuid, observer.getName(), propertyMap);

            EnumSet<PlayerListS2CPacket.Action> actions = EnumSet.of(
                    PlayerListS2CPacket.Action.ADD_PLAYER,
                    PlayerListS2CPacket.Action.UPDATE_LISTED,
                    PlayerListS2CPacket.Action.UPDATE_GAME_MODE,
                    PlayerListS2CPacket.Action.UPDATE_LATENCY,
                    PlayerListS2CPacket.Action.UPDATE_HAT
            );

            RegistryByteBuf buf = new RegistryByteBuf(
                    Unpooled.buffer(),
                    server.getRegistryManager()
            );

            buf.writeEnumSet(actions, PlayerListS2CPacket.Action.class);
            buf.writeVarInt(1);
            buf.writeUuid(uuid);

            // ADD_PLAYER
            PacketCodecs.PLAYER_NAME.encode(buf, profile.name());
            PacketCodecs.PROPERTY_MAP.encode(buf, propertyMap);

            // UPDATE_LISTED
            buf.writeBoolean(true);

            // UPDATE_GAME_MODE
            buf.writeVarInt(GameMode.SURVIVAL.getIndex());

            // UPDATE_LATENCY
            buf.writeVarInt(0);

            // UPDATE_HAT
            buf.writeBoolean(true);

            PlayerListS2CPacket packet = PlayerListS2CPacket.CODEC.decode(buf);
            buf.release();
            return packet;

        } catch (Exception e) {
            Residue.LOGGER.warn("[Residue] Failed to build tab list packet for {}: {}",
                    observer.getName(), e.getMessage());
            return null;
        }
    }

    private static void broadcast(MinecraftServer server, PlayerListS2CPacket packet) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(packet);
        }
    }
}