package com.upphorattexistera.residue.observer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;
import com.upphorattexistera.residue.Residue;
import com.upphorattexistera.residue.config.ResidueConfig;
import com.upphorattexistera.residue.memory.MemoryManager;
import com.upphorattexistera.residue.observer.persona.ObserverAssignment;
import com.upphorattexistera.residue.observer.persona.ObserverDataStore;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ObserverSkinResolver {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Random RANDOM = new Random();

    // Кеш: имя → SkinData (чтобы не делать повторные запросы)
    private static final ConcurrentHashMap<String, SkinData> cache =
            new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // Публичный API
    // ----------------------------------------------------------------

    public static CompletableFuture<SkinData> resolve(String name) {

        if (cache.containsKey(name)) {
            return CompletableFuture.completedFuture(cache.get(name));
        }

        return CompletableFuture
                .supplyAsync(() -> tryMojang(name))
                .thenCompose(result -> {
                    if (result.hasTextures()) {
                        return CompletableFuture.completedFuture(result);
                    }
                    return CompletableFuture.supplyAsync(() -> tryElyBy(name));
                })
                .thenApply(result -> {
                    if (result.hasTextures()) {
                        cache.put(name, result);
                        return result;
                    }
                    SkinData local = tryLocal(name);
                    cache.put(name, local);
                    return local;
                });
    }

    public static SkinData getCached(String name) {
        return cache.getOrDefault(name, SkinData.unknown());
    }

    /**
     * Сбрасывает кеш скинов при остановке сервера.
     * Привязки assignedSkinFiles НЕ сбрасываем — обсервер
     * должен сохранять свой файл между сессиями в рамках
     * одного запуска сервера.
     */
    public static void clearCache() {
        cache.clear();
    }

    // ----------------------------------------------------------------
    // Mojang
    // ----------------------------------------------------------------

    private static SkinData tryMojang(String name) {
        try {
            String profileJson = get(
                    "https://api.mojang.com/users/profiles/minecraft/" + name
            );
            if (profileJson == null) return SkinData.unknown();

            JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
            String uuid = profile.get("id").getAsString();

            String sessionJson = get(
                    "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid
            );
            if (sessionJson == null) return SkinData.unknown();

            return extractSkinProperty(sessionJson, SkinData.Source.MOJANG);

        } catch (Exception e) {
            Residue.LOGGER.debug("[Residue] Mojang skin lookup failed for {}: {}",
                    name, e.getMessage());
            return SkinData.unknown();
        }
    }

    // ----------------------------------------------------------------
    // Ely.by
    // ----------------------------------------------------------------

    private static SkinData tryElyBy(String name) {
        try {
            String profileJson = get(
                    "http://skinsystem.ely.by/profile/" + name
            );
            if (profileJson == null) return SkinData.unknown();

            return extractSkinProperty(profileJson, SkinData.Source.ELY_BY);

        } catch (Exception e) {
            Residue.LOGGER.debug("[Residue] Ely.by skin lookup failed for {}: {}",
                    name, e.getMessage());
            return SkinData.unknown();
        }
    }

    // ----------------------------------------------------------------
    // Локальные папки
    // ----------------------------------------------------------------

    private static SkinData tryLocal(String name) {
        try {
            int memory = MemoryManager.getMemory();
            int max = ResidueConfig.INSTANCE.maxMemory;
            int stage = getStage(memory, max);

            Optional<ModContainer> mod =
                    FabricLoader.getInstance().getModContainer("residue");
            if (mod.isEmpty()) return SkinData.unknown();

            Optional<Path> dirOpt =
                    mod.get().findPath("assets/residue/skins/stage_" + stage);
            if (dirOpt.isEmpty()) return SkinData.unknown();

            Path dir = dirOpt.get();
            if (!Files.isDirectory(dir)) return SkinData.unknown();

            List<Path> skins = new ArrayList<>();
            try (var stream = Files.list(dir)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".png"))
                        .sorted()
                        .forEach(skins::add);
            }
            if (skins.isEmpty()) return SkinData.unknown();

            Path skinPath = null;
            ObserverAssignment assignment = ObserverDataStore.get(name);

            // Если привязка есть и стадия совпадает — ищем тот же файл
            if (assignment != null && assignment.skinFile != null
                    && assignment.skinStage == stage) {
                skinPath = skins.stream()
                        .filter(p -> p.getFileName().toString().equals(assignment.skinFile))
                        .findFirst()
                        .orElse(null);
            }

            // Не нашли — выбираем случайный и сохраняем
            if (skinPath == null) {
                skinPath = skins.get(RANDOM.nextInt(skins.size()));
                String chosen = skinPath.getFileName().toString();
                ObserverDataStore.updateSkin(name, chosen, stage);
                Residue.LOGGER.debug("[Residue] {} → new skin: stage_{}/{}",
                        name, stage, chosen);
            }

            return buildSkinData(name, skinPath);

        } catch (Exception e) {
            Residue.LOGGER.debug("[Residue] Local skin lookup failed for {}: {}",
                    name, e.getMessage());
            return SkinData.unknown();
        }
    }

    /**
     * Строит SkinData из PNG файла.
     * Определяет модель по имени файла:
     *   slim_XX.png    → Alex (тонкие руки)
     *   default_XX.png → Steve (широкие руки)
     */
    private static SkinData buildSkinData(String name, Path skinPath) throws Exception {

        byte[] skinBytes = Files.readAllBytes(skinPath);
        String skinBase64 = Base64.getEncoder().encodeToString(skinBytes);

        boolean isSlim = skinPath.getFileName().toString().startsWith("slim_");

        JsonObject skinObj = new JsonObject();
        skinObj.addProperty("url", "data:image/png;base64," + skinBase64);

        if (isSlim) {
            JsonObject metadata = new JsonObject();
            metadata.addProperty("model", "slim");
            skinObj.add("metadata", metadata);
        }

        JsonObject texturesObj = new JsonObject();
        texturesObj.add("SKIN", skinObj);

        JsonObject root = new JsonObject();
        root.addProperty("timestamp", System.currentTimeMillis());
        root.addProperty("profileId",
                ObserverTabListManager.uuidFromName(name)
                        .toString().replace("-", ""));
        root.addProperty("profileName", name);
        root.add("textures", texturesObj);

        String value = Base64.getEncoder().encodeToString(
                root.toString().getBytes(StandardCharsets.UTF_8)
        );

        Property property = new Property("textures", value, null);
        SkinData.Model model = isSlim ? SkinData.Model.SLIM : SkinData.Model.DEFAULT;

        return new SkinData(property, model, SkinData.Source.LOCAL);
    }

    // ----------------------------------------------------------------
    // Утилиты
    // ----------------------------------------------------------------

    private static SkinData extractSkinProperty(String sessionJson,
                                                SkinData.Source source) {
        try {
            JsonObject session = JsonParser.parseString(sessionJson).getAsJsonObject();
            var properties = session.getAsJsonArray("properties");

            for (var element : properties) {
                JsonObject prop = element.getAsJsonObject();
                if (!"textures".equals(prop.get("name").getAsString())) continue;

                String value = prop.get("value").getAsString();
                String signature = prop.has("signature")
                        ? prop.get("signature").getAsString()
                        : null;

                String decoded = new String(
                        Base64.getDecoder().decode(value),
                        StandardCharsets.UTF_8
                );

                JsonObject textures = JsonParser.parseString(decoded)
                        .getAsJsonObject()
                        .getAsJsonObject("textures");

                SkinData.Model model = SkinData.Model.DEFAULT;

                if (textures != null && textures.has("SKIN")) {
                    JsonObject skin = textures.getAsJsonObject("SKIN");
                    if (skin.has("metadata")) {
                        String modelStr = skin.getAsJsonObject("metadata")
                                .get("model").getAsString();
                        if ("slim".equals(modelStr)) {
                            model = SkinData.Model.SLIM;
                        }
                    }
                }

                Property property = new Property("textures", value, signature);
                return new SkinData(property, model, source);
            }

            return SkinData.unknown();

        } catch (Exception e) {
            return SkinData.unknown();
        }
    }

    private static String get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.statusCode() == 200 ? response.body() : null;

        } catch (Exception e) {
            return null;
        }
    }

    private static int getStage(int memory, int max) {
        if (memory < max * 0.20) return 0;
        if (memory < max * 0.40) return 1;
        if (memory < max * 0.60) return 2;
        if (memory < max * 0.80) return 3;
        return 4;
    }
}