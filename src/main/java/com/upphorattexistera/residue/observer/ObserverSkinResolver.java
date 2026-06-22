package com.upphorattexistera.residue.observer;

import com.google.gson.JsonObject;
import com.upphorattexistera.residue.Residue;
import com.upphorattexistera.residue.config.ResidueConfig;
import com.upphorattexistera.residue.memory.MemoryManager;
import com.upphorattexistera.residue.observer.persona.ObserverAssignment;
import com.upphorattexistera.residue.observer.persona.ObserverDataStore;
import com.mojang.authlib.properties.Property;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

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

    private static final Random RANDOM = new Random();

    // Кеш: имя → SkinData
    private static final ConcurrentHashMap<String, SkinData> cache =
            new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // Публичный API
    // ----------------------------------------------------------------

    /**
     * Резолвит скин обсервера — только из локальных ресурсов мода.
     * Возвращает CompletableFuture для совместимости с остальным кодом.
     */
    public static CompletableFuture<SkinData> resolve(String name) {
        if (cache.containsKey(name)) {
            return CompletableFuture.completedFuture(cache.get(name));
        }

        return CompletableFuture.supplyAsync(() -> {
            SkinData local = tryLocal(name);
            cache.put(name, local);
            return local;
        });
    }

    public static SkinData getCached(String name) {
        return cache.getOrDefault(name, SkinData.unknown());
    }

    public static void clearCache() {
        cache.clear();
    }

    // ----------------------------------------------------------------
    // Локальные скины
    // ----------------------------------------------------------------

    private static SkinData tryLocal(String name) {
        try {
            int memory = MemoryManager.getMemory();
            int max = ResidueConfig.INSTANCE.maxMemory;
            int stage = getStage(memory, max);

            Optional<ModContainer> mod =
                    FabricLoader.getInstance().getModContainer("residue");
            if (mod.isEmpty()) return SkinData.unknown();

            // Пробуем стейджи от текущего до 0, пока не найдём скины
            for (int s = stage; s >= 0; s--) {
                Optional<Path> dirOpt = mod.get().findPath("assets/residue/skins/stage_" + s);
                if (dirOpt.isEmpty()) continue;

                Path dir = dirOpt.get();
                if (!Files.isDirectory(dir)) continue;

                List<Path> skins = new ArrayList<>();
                try (var stream = Files.list(dir)) {
                    stream.filter(p -> p.getFileName().toString().endsWith(".png"))
                            .sorted()
                            .forEach(skins::add);
                }
                if (skins.isEmpty()) continue;

                return pickAndBuildSkin(name, skins, stage);
            }

            Residue.LOGGER.warn("[Residue] No local skins found for observer: {}", name);
            return SkinData.unknown();

        } catch (Exception e) {
            Residue.LOGGER.warn("[Residue] Local skin lookup failed for {}: {}",
                    name, e.getMessage());
            return SkinData.unknown();
        }
    }

    /**
     * Выбирает скин из списка с учётом привязки обсервера.
     * Если обсервер уже привязан к файлу на этой стадии — берём тот же файл.
     */
    private static SkinData pickAndBuildSkin(String name, List<Path> skins, int stage) throws Exception {
        Path skinPath = null;
        ObserverAssignment assignment = ObserverDataStore.get(name);

        if (assignment != null
                && assignment.skinFile != null
                && !assignment.skinFile.isEmpty()
                && assignment.skinStage == stage) {
            skinPath = skins.stream()
                    .filter(p -> p.getFileName().toString().equals(assignment.skinFile))
                    .findFirst()
                    .orElse(null);
        }

        if (skinPath == null) {
            skinPath = skins.get(RANDOM.nextInt(skins.size()));
            String chosen = skinPath.getFileName().toString();
            ObserverDataStore.updateSkin(name, chosen, stage);
            Residue.LOGGER.debug("[Residue] {} → new skin: stage_{}/{}",
                    name, stage, chosen);
        }

        return buildSkinData(name, skinPath);
    }

    /**
     * Строит SkinData из PNG файла.
     * Соглашение по именам файлов:
     *   slim_*.png    → Alex (тонкие руки)
     *   default_*.png → Steve (широкие руки)
     *   Всё остальное → Steve
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
                root.toString().getBytes(StandardCharsets.UTF_8));

        Property property = new Property("textures", value, null);
        SkinData.Model model = isSlim ? SkinData.Model.SLIM : SkinData.Model.DEFAULT;

        return new SkinData(property, model, SkinData.Source.LOCAL);
    }

    // ----------------------------------------------------------------
    // Утилиты
    // ----------------------------------------------------------------

    private static int getStage(int memory, int max) {
        if (memory < max * 0.20) return 0;
        if (memory < max * 0.40) return 1;
        if (memory < max * 0.60) return 2;
        if (memory < max * 0.80) return 3;
        return 4;
    }
}