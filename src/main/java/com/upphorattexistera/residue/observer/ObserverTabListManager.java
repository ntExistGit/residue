package com.upphorattexistera.residue.observer;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.upphorattexistera.residue.Residue;
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

    /**
     * РУЧНАЯ СБОРКА PlayerListS2CPacket.
     *
     * Причина: PlayerListS2CPacket не имеет публичного конструктора,
     * принимающего произвольный Entry с кастомным GameProfile/скином —
     * единственный способ создать "виртуального" игрока в tab-list — это
     * собрать байты в том же порядке, в котором их ожидает PlayerListS2CPacket.CODEC.
     *
     * ВАЖНО ДЛЯ БУДУЩИХ ОБНОВЛЕНИЙ МАППИНГОВ (Modern Yarn):
     * Зафиксировано под minecraft_version=26.1.2, yarn_mappings=26.1.2+build.3.
     * Если после апдейта маппингов или версии MC игроки-обсерверы перестали
     * появляться в tab-листе (или клиент крашится при decode/encode этого
     * пакета) — ПЕРВЫМ ДЕЛОм проверяйте порядок полей ниже, сверяя его
     * с реализацией PlayerListS2CPacket.CODEC в декомпилированном клиенте
     * новой версии. Порядок полей ОБЯЗАН совпадать байт-в-байт:
     *
     *   1. EnumSet<Action>                  — writeEnumSet
     *   2. varint: количество записей (= 1)
     *   3. UUID игрока
     *   4. [ADD_PLAYER]      имя профиля     — PacketCodecs.PLAYER_NAME
     *   5. [ADD_PLAYER]      PropertyMap     — PacketCodecs.PROPERTY_MAP
     *   6. [UPDATE_LISTED]   boolean
     *   7. [UPDATE_GAME_MODE] varint (GameMode.getIndex())
     *   8. [UPDATE_LATENCY]  varint (ping)
     *   9. [UPDATE_HAT]      boolean
     *
     * Порядок полей 4-9 соответствует порядку самого EnumSet ACTIONS ниже —
     * если поменяете порядок ACTIONS, обязаны поменять и порядок записи байт.
     */
    private static final EnumSet<PlayerListS2CPacket.Action> ACTIONS = EnumSet.of(
            PlayerListS2CPacket.Action.ADD_PLAYER,
            PlayerListS2CPacket.Action.UPDATE_LISTED,
            PlayerListS2CPacket.Action.UPDATE_GAME_MODE,
            PlayerListS2CPacket.Action.UPDATE_LATENCY,
            PlayerListS2CPacket.Action.UPDATE_HAT
    );

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

        RegistryByteBuf buf = null;

        try {
            UUID uuid = uuidFromName(observer.getName());

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

            buf = new RegistryByteBuf(
                    Unpooled.buffer(),
                    server.getRegistryManager()
            );

            // См. javadoc класса — порядок полей ниже зафиксирован под текущие маппинги.
            buf.writeEnumSet(ACTIONS, PlayerListS2CPacket.Action.class);
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

            return PlayerListS2CPacket.CODEC.decode(buf);

        } catch (Exception e) {
            Residue.LOGGER.error(
                    "[Residue] Failed to build tab list packet for '{}'.",
                    observer.getName(), e);
            return null;
        } finally {
            if (buf != null) buf.release();
        }
    }

    private static void broadcast(MinecraftServer server, PlayerListS2CPacket packet) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(packet);
        }
    }
}