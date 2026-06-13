package com.upphorattexistera.residue.observer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;
import com.upphorattexistera.residue.Residue;
import com.upphorattexistera.residue.config.ResidueConfig;
import com.upphorattexistera.residue.memory.MemoryManager;
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

    // Привязка: имя обсервера → имя файла скина (например "slim_03.png")
    // Сохраняется на всё время работы сервера
    private static final ConcurrentHashMap<String, String> assignedSkinFiles =
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

    /**
     * Полный сброс — вызывать только если нужно
     * переназначить скины всем обсерверам заново.
     */
    public static void clearAll() {
        cache.clear();
        assignedSkinFiles.clear();
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

            // Собираем все PNG файлы в папке стейджа
            List<Path> skins = new ArrayList<>();
            try (var stream = Files.list(dir)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".png"))
                        .sorted() // стабильный порядок
                        .forEach(skins::add);
            }

            if (skins.isEmpty()) return SkinData.unknown();

            // Определяем файл скина для этого обсервера
            Path skinPath = resolveSkinPath(name, stage, skins, dir, mod.get());

            if (skinPath == null) return SkinData.unknown();

            Residue.LOGGER.debug("[Residue] Local skin for {}: stage_{}/{}",
                    name, stage, skinPath.getFileName());

            return buildSkinData(name, skinPath);

        } catch (Exception e) {
            Residue.LOGGER.debug("[Residue] Local skin lookup failed for {}: {}",
                    name, e.getMessage());
            return SkinData.unknown();
        }
    }

    /**
     * Определяет путь к файлу скина для обсервера.
     *
     * Логика:
     * 1. Если обсервер уже получал скин — ищем тот же файл в текущем стейдже.
     * 2. Если файл найден — используем его (скин сохраняется между стейджами).
     * 3. Если файл не найден (стейдж сменился и такого файла нет) — выбираем
     *    случайный из текущего стейджа и запоминаем.
     * 4. Если обсервер новый — выбираем случайный и запоминаем.
     */
    private static Path resolveSkinPath(String name, int stage,
                                        List<Path> skins, Path dir,
                                        ModContainer mod) {

        String assignedFile = assignedSkinFiles.get(name);

        if (assignedFile != null) {
            // Ищем тот же файл в текущем стейдже
            Path existing = skins.stream()
                    .filter(p -> p.getFileName().toString().equals(assignedFile))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                Residue.LOGGER.debug(
                        "[Residue] {} → reusing assigned skin: {}",
                        name, assignedFile);
                return existing;
            }

            // Файл не найден в текущем стейдже — выбираем новый случайный
            Residue.LOGGER.debug(
                    "[Residue] {} → assigned skin '{}' not found in stage_{}, picking new",
                    name, assignedFile, stage);
        }

        // Первое подключение или файл не найден — случайный выбор
        Path chosen = skins.get(RANDOM.nextInt(skins.size()));
        String chosenFileName = chosen.getFileName().toString();
        assignedSkinFiles.put(name, chosenFileName);

        Residue.LOGGER.debug(
                "[Residue] {} → assigned new skin: stage_{}/{}",
                name, stage, chosenFileName);

        return chosen;
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