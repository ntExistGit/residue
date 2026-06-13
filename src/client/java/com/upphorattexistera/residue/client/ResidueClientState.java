package com.upphorattexistera.residue.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.upphorattexistera.residue.client.observer.ObserverEntityManager;
import com.upphorattexistera.residue.network.ObserverListPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void updateObservers(List<ObserverListPacket.ObserverEntry> newList) {
        observers.clear();
        observers.addAll(newList);

        for (ObserverListPacket.ObserverEntry entry : newList) {
            skinSlimMap.put(entry.uuid(), entry.slim());

            if (entry.skinTextureId() != null && !skinTextures.containsKey(entry.uuid())) {
                registerSkin(entry.uuid(), entry.name(), entry.skinTextureId());
            }
        }

        ObserverEntityManager.sync(newList);
    }

    public static boolean isObserverSlim(UUID uuid) {
        return skinSlimMap.getOrDefault(uuid, false);
    }

    private static void registerSkin(UUID uuid, String name, String base64Value) {
        // Запускаем загрузку в отдельном потоке
        Thread.ofVirtual().name("residue-skin-" + name).start(() -> {
            try {
                String decoded = new String(
                        Base64.getDecoder().decode(base64Value),
                        StandardCharsets.UTF_8
                );

                JsonObject root = JsonParser.parseString(decoded).getAsJsonObject();
                JsonObject textures = root.getAsJsonObject("textures");
                if (textures == null || !textures.has("SKIN")) return;

                String skinUrl = textures.getAsJsonObject("SKIN")
                        .get("url").getAsString();

                byte[] imageBytes;

                if (skinUrl.startsWith("data:image/png;base64,")) {
                    // Локальный скин — уже есть байты
                    String imageBase64 = skinUrl.substring(
                            "data:image/png;base64,".length());
                    imageBytes = Base64.getDecoder().decode(imageBase64);
                } else {
                    // Удалённый URL — скачиваем
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(skinUrl))
                            .GET()
                            .build();
                    HttpResponse<byte[]> response = HTTP.send(
                            request,
                            HttpResponse.BodyHandlers.ofByteArray()
                    );
                    if (response.statusCode() != 200) return;
                    imageBytes = response.body();
                }

                final byte[] finalBytes = imageBytes;

                // Регистрируем текстуру в главном потоке
                MinecraftClient.getInstance().execute(() -> {
                    try {
                        NativeImage image = NativeImage.read(
                                new ByteArrayInputStream(finalBytes));
                        NativeImageBackedTexture texture =
                                new NativeImageBackedTexture(
                                        () -> "residue_observer_" + name, image);
                        Identifier id = Identifier.of(
                                "residue", "observer_skin/" + name.toLowerCase());
                        MinecraftClient.getInstance()
                                .getTextureManager()
                                .registerTexture(id, texture);
                        skinTextures.put(uuid, id);
                        System.out.println("[Residue] Skin registered for " + name
                                + " → " + id);
                    } catch (Exception e) {
                        System.err.println("[Residue] Failed to register skin for "
                                + name + ": " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                System.err.println("[Residue] Failed to load skin for "
                        + name + ": " + e.getMessage());
            }
        });
    }

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

    public static void clear() {
        observers.clear();
        skinTextures.clear();
        skinSlimMap.clear();
    }
}