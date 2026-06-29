package com.upphorattexistera.residue.client;

import com.upphorattexistera.residue.Residue;
import com.upphorattexistera.residue.network.ObserverListPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ResidueClientState {

    private static final List<ObserverListPacket.ObserverEntry> observers =
            new CopyOnWriteArrayList<>();

    private static final Map<UUID, Identifier> skinTextures = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> skinSlimMap = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // Обновление списка обсерверов
    // ----------------------------------------------------------------

    public static void updateObservers(List<ObserverListPacket.ObserverEntry> newList) {
        observers.clear();
        observers.addAll(newList);

        for (ObserverListPacket.ObserverEntry entry : newList) {
            skinSlimMap.put(entry.uuid(), entry.slim());

            if (entry.skinTextureId() != null && !skinTextures.containsKey(entry.uuid())) {
                registerSkin(entry.uuid(), entry.name(), entry.skinTextureId());
            }
        }
    }

    // ----------------------------------------------------------------
    // Регистрация скина (асинхронный путь — пакет от сервера)
    // ----------------------------------------------------------------

    private static void registerSkin(UUID uuid, String name, String base64Value) {
        Thread.ofVirtual().name("residue-skin-" + name).start(() -> {
            InlineSkinTextureDecoder.decode(base64Value, name).ifPresent(decoded ->
                    MinecraftClient.getInstance().execute(() ->
                            registerNativeTexture(uuid, name, decoded.imageBytes(), decoded.isSlim()))
            );
        });
    }

    /**
     * Регистрирует уже декодированный скин напрямую, минуя пакетный пайплайн.
     * Используется MixinPlayerSkinProvider, когда скин пришёл через fallback
     * (property GameProfile) раньше, чем отработал ObserverListPacket —
     * чтобы при повторных вызовах fetchSkinTextures не декодировать
     * одну и ту же текстуру заново.
     *
     * Должен вызываться на главном (render) потоке.
     */
    public static void registerDecodedSkin(UUID uuid, Identifier textureId, boolean isSlim) {
        skinTextures.put(uuid, textureId);
        skinSlimMap.put(uuid, isSlim);
    }

    private static void registerNativeTexture(UUID uuid, String name, byte[] imageBytes, boolean isSlim) {
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(imageBytes));
            NativeImageBackedTexture texture =
                    new NativeImageBackedTexture(() -> "residue_observer_" + name, image);
            Identifier id = Identifier.of("residue", "observer_skin/" + name.toLowerCase());

            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);

            skinTextures.put(uuid, id);
            skinSlimMap.put(uuid, isSlim);

            Residue.LOGGER.debug("[Residue] Skin registered for {} → {}", name, id);
        } catch (Exception e) {
            Residue.LOGGER.warn("[Residue] Failed to register skin for {}: {}", name, e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Геттеры
    // ----------------------------------------------------------------

    public static List<ObserverListPacket.ObserverEntry> getObservers() {
        return Collections.unmodifiableList(observers);
    }

    public static boolean isObserver(UUID uuid) {
        return observers.stream().anyMatch(e -> e.uuid().equals(uuid));
    }

    public static int getObserverLatency(UUID uuid) {
        return observers.stream()
                .filter(e -> e.uuid().equals(uuid))
                .mapToInt(ObserverListPacket.ObserverEntry::latency)
                .findFirst()
                .orElse(0);
    }

    public static Identifier getObserverSkinTexture(UUID uuid) {
        return skinTextures.get(uuid);
    }

    public static boolean isObserverSlim(UUID uuid) {
        return skinSlimMap.getOrDefault(uuid, false);
    }

    public static void clear() {
        observers.clear();
        skinTextures.clear();
        skinSlimMap.clear();
    }
}