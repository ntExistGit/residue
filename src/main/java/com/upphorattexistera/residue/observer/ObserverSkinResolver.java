package com.upphorattexistera.residue.observer;

import com.google.gson.JsonObject;
import com.upphorattexistera.residue.Residue;
import com.upphorattexistera.residue.config.ResidueConfig;
import com.upphorattexistera.residue.memory.MemoryManager;
import com.upphorattexistera.residue.memory.MemoryStage;
import com.upphorattexistera.residue.observer.persona.ObserverAssignment;
import com.upphorattexistera.residue.observer.persona.ObserverDataStore;
import com.upphorattexistera.residue.observer.persona.ObserverGender;
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

    private static final ConcurrentHashMap<String, SkinData> cache =
            new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // Публичный API
    // ----------------------------------------------------------------

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
            int stage = MemoryStage.getStage(memory, max);

            Optional<ModContainer> mod =
                    FabricLoader.getInstance().getModContainer("residue");
            if (mod.isEmpty()) return SkinData.unknown();

            // Гарантируем существование привязки (персона + пол) ДО выбора
            // файла скина — нужно знать пол, чтобы фильтровать кандидатов
            // по префиксу имени файла. Идемпотентно: если привязка уже
            // существует (создана при executeConnect), просто вернёт её.
            ObserverAssignment assignment = ObserverDataStore.getOrCreate(name, "");
            ObserverGender gender = ObserverGender.byId(assignment.gender);

            // Пробуем стейджи от текущего до 0, пока не найдём скины
            // подходящего пола.
            for (int s = stage; s >= 0; s--) {
                Optional<Path> dirOpt = mod.get().findPath("assets/residue/skins/stage_" + s);
                if (dirOpt.isEmpty()) continue;

                Path dir = dirOpt.get();
                if (!Files.isDirectory(dir)) continue;

                List<Path> allSkins = new ArrayList<>();
                try (var stream = Files.list(dir)) {
                    stream.filter(p -> p.getFileName().toString().endsWith(".png"))
                            .sorted()
                            .forEach(allSkins::add);
                }
                if (allSkins.isEmpty()) continue;

                List<Path> genderSkins = allSkins.stream()
                        .filter(p -> matchesGender(p.getFileName().toString(), gender))
                        .toList();

                if (genderSkins.isEmpty()) continue; // нет файлов этого пола — пробуем стейдж ниже

                return pickAndBuildSkin(name, assignment, genderSkins, s);
            }

            Residue.LOGGER.warn("[Residue] No local skins found for observer: {} (gender={})",
                    name, gender.filePrefix);
            return SkinData.unknown();

        } catch (Exception e) {
            Residue.LOGGER.warn("[Residue] Local skin lookup failed for {}: {}",
                    name, e.getMessage());
            return SkinData.unknown();
        }
    }

    private static boolean matchesGender(String fileName, ObserverGender gender) {
        return fileName.toLowerCase().startsWith(gender.filePrefix + "_");
    }

    /**
     * Выбирает конкретный файл скина среди подходящих по полу кандидатов.
     *
     * Главное правило — сохранение "личности" скина между стейджами:
     * если у обсервера уже есть назначенный файл (assignment.skinFile),
     * сначала ищем файл С ТЕМ ЖЕ ИМЕНЕМ в текущей папке стадии. Только
     * если такого файла там нет — выбираем случайный среди подходящих
     * по полу. Раньше реюз срабатывал только при ТОЧНОМ совпадении
     * стадии (assignment.skinStage == stage), из-за чего при каждой
     * смене стейджа обсервер визуально "превращался в другого человека".
     */
    private static SkinData pickAndBuildSkin(String name, ObserverAssignment assignment,
                                             List<Path> genderSkins, int foundStage) throws Exception {
        Path skinPath = null;

        if (assignment.skinFile != null && !assignment.skinFile.isEmpty()) {
            skinPath = genderSkins.stream()
                    .filter(p -> p.getFileName().toString().equals(assignment.skinFile))
                    .findFirst()
                    .orElse(null);
        }

        if (skinPath == null) {
            skinPath = genderSkins.get(RANDOM.nextInt(genderSkins.size()));
            String chosen = skinPath.getFileName().toString();
            ObserverDataStore.updateSkin(name, chosen, foundStage);
            Residue.LOGGER.debug("[Residue] {} → new skin: stage_{}/{}",
                    name, foundStage, chosen);
        } else if (assignment.skinStage != foundStage) {
            // Тот же файл, что и раньше — просто переносим метку стадии,
            // имя файла не меняется.
            ObserverDataStore.updateSkin(name, assignment.skinFile, foundStage);
            Residue.LOGGER.debug("[Residue] {} → carried skin over to stage_{}/{}",
                    name, foundStage, assignment.skinFile);
        }

        return buildSkinData(name, skinPath);
    }

    /**
     * Соглашение по именам файлов: <пол>_<модель>_<номер>.png
     * например: f_slim_01.png, m_slim_01.png, b_wide_01.png.
     * Часть после префикса пола определяет модель:
     *   slim_*  → Alex (тонкие руки)
     *   всё остальное → Steve (широкие руки)
     */
    private static SkinData buildSkinData(String name, Path skinPath) throws Exception {
        byte[] skinBytes = Files.readAllBytes(skinPath);
        String skinBase64 = Base64.getEncoder().encodeToString(skinBytes);

        String fileName = skinPath.getFileName().toString();
        int genderSepIndex = fileName.indexOf('_');
        String afterGenderPrefix = genderSepIndex >= 0
                ? fileName.substring(genderSepIndex + 1)
                : fileName;
        boolean isSlim = afterGenderPrefix.toLowerCase().startsWith("slim");

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
}