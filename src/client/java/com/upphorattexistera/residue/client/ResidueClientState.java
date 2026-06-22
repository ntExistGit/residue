package com.upphorattexistera.residue.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.upphorattexistera.residue.Residue;
import com.upphorattexistera.residue.network.ObserverListPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
    // Регистрация скина
    // ----------------------------------------------------------------

    /**
     * Декодирует base64-значение property "textures" и регистрирует текстуру.
     * Поскольку используются только локальные скины, URL всегда имеет вид:
     *   data:image/png;base64,<bytes>
     */
    private static void registerSkin(UUID uuid, String name, String base64Value) {
        Thread.ofVirtual().name("residue-skin-" + name).start(() -> {
            try {
                // Декодируем JSON из base64-значения property
                String decoded = new String(
                        Base64.getDecoder().decode(base64Value),
                        StandardCharsets.UTF_8);

                JsonObject root = JsonParser.parseString(decoded).getAsJsonObject();
                JsonObject textures = root.getAsJsonObject("textures");

                if (textures == null || !textures.has("SKIN")) {
                    Residue.LOGGER.warn("[Residue] No SKIN in textures for {}", name);
                    return;
                }

                String skinUrl = textures.getAsJsonObject("SKIN")
                        .get("url").getAsString();

                // Только data URI — никаких внешних запросов
                if (!skinUrl.startsWith("data:image/png;base64,")) {
                    Residue.LOGGER.warn("[Residue] Unexpected skin URL format for {}", name);
                    return;
                }

                String imageBase64 = skinUrl.substring("data:image/png;base64,".length());
                byte[] imageBytes = Base64.getDecoder().decode(imageBase64);

                // Регистрируем текстуру в главном потоке
                MinecraftClient.getInstance().execute(() -> {
                    try {
                        NativeImage image = NativeImage.read(
                                new ByteArrayInputStream(imageBytes));
                        NativeImageBackedTexture texture =
                                new NativeImageBackedTexture(
                                        () -> "residue_observer_" + name, image);
                        Identifier id = Identifier.of(
                                "residue", "observer_skin/" + name.toLowerCase());
                        MinecraftClient.getInstance()
                                .getTextureManager()
                                .registerTexture(id, texture);
                        skinTextures.put(uuid, id);
                        Residue.LOGGER.debug("[Residue] Skin registered for {} → {}",
                                name, id);
                    } catch (Exception e) {
                        Residue.LOGGER.warn("[Residue] Failed to register skin for {}: {}",
                                name, e.getMessage());
                    }
                });

            } catch (Exception e) {
                Residue.LOGGER.warn("[Residue] Failed to decode skin for {}: {}",
                        name, e.getMessage());
            }
        });
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