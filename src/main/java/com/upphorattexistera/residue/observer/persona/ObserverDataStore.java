package com.upphorattexistera.residue.observer.persona;

import com.google.gson.*;
import com.upphorattexistera.residue.Residue;
import com.upphorattexistera.residue.config.ResidueConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ObserverDataStore {

    // name → assignment
    private static final Map<String, ObserverAssignment> assignments =
            new ConcurrentHashMap<>();

    private static Path savePath;
    private static final int MAX_HISTORY_SIZE = ResidueConfig.INSTANCE.maxHistorySize;

    // ----------------------------------------------------------------
    // Init
    // ----------------------------------------------------------------

    public static void onServerStarted(MinecraftServer server) {
        savePath = server.getSavePath(WorldSavePath.ROOT)
                .resolve("residue/observer_data.json");
        load();
    }

    public static void onServerStopping() {
        save();
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Возвращает привязку для обсервера.
     * Если её нет — создаёт новую с рандомной персоной.
     */
    public static ObserverAssignment getOrCreate(String observerName, String skinFile) {
        return assignments.computeIfAbsent(observerName, name -> {
            ObserverPersona persona = ObserverPersonaLoader.getRandom();
            int personaId = persona != null ? persona.id : 0;

            Residue.LOGGER.info("[Residue] New observer assignment: {} → persona={}",
                    name, personaId);

            return new ObserverAssignment(name, personaId, skinFile, 0);
        });
    }

    public static ObserverAssignment get(String observerName) {
        return assignments.get(observerName);
    }

    /**
     * Обновляет skinFile если стадия изменилась.
     */
    public static void updateSkin(String observerName, String skinFile, int skinStage) {
        ObserverAssignment assignment = assignments.get(observerName);
        if (assignment != null) {
            assignment.skinFile = skinFile;
            assignment.skinStage = skinStage;
        }
    }

    /**
     * Добавляет сообщение в историю диалога.
     */
    public static void addToHistory(String observerName, String role, String content) {
        ObserverAssignment assignment = assignments.get(observerName);
        if (assignment == null) return;

        JsonObject msg = new JsonObject();
        msg.addProperty("role", role);
        msg.addProperty("content", content);
        assignment.conversationHistory.add(msg);

        // Ограничиваем размер
        while (assignment.conversationHistory.size() > MAX_HISTORY_SIZE) {
            assignment.conversationHistory.remove(0);
        }
    }

    public static JsonArray getHistory(String observerName) {
        ObserverAssignment assignment = assignments.get(observerName);
        return assignment != null ? assignment.conversationHistory : new JsonArray();
    }

    // ----------------------------------------------------------------
    // Save / Load
    // ----------------------------------------------------------------

    public static void save() {
        if (savePath == null) return;
        try {
            Files.createDirectories(savePath.getParent());

            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();

            for (ObserverAssignment a : assignments.values()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", a.name);
                obj.addProperty("persona_id", a.personaId);
                obj.addProperty("skin_file", a.skinFile);
                obj.addProperty("skin_stage", a.skinStage);
                obj.add("conversation_history", a.conversationHistory);
                arr.add(obj);
            }

            root.add("observers", arr);

            Files.writeString(savePath,
                    new GsonBuilder().setPrettyPrinting().create().toJson(root),
                    StandardCharsets.UTF_8);

            Residue.LOGGER.debug("[Residue] ObserverDataStore saved ({} entries)",
                    assignments.size());

        } catch (Exception e) {
            Residue.LOGGER.warn("[Residue] Failed to save observer_data.json: {}",
                    e.getMessage());
        }
    }

    private static void load() {
        assignments.clear();
        if (savePath == null || !Files.exists(savePath)) return;

        try {
            String json = Files.readString(savePath, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            for (JsonElement elem : root.getAsJsonArray("observers")) {
                JsonObject obj = elem.getAsJsonObject();
                String name = obj.get("name").getAsString();
                int personaId = obj.get("persona_id").getAsInt();
                String skinFile = obj.get("skin_file").getAsString();
                int skinStage = obj.get("skin_stage").getAsInt();

                ObserverAssignment assignment = new ObserverAssignment(
                        name, personaId, skinFile, skinStage);

                if (obj.has("conversation_history")) {
                    assignment.conversationHistory =
                            obj.get("conversation_history").getAsJsonArray();
                }

                assignments.put(name, assignment);
                Residue.LOGGER.debug("[Residue] Loaded observer: {} persona={}",
                        name, personaId);
            }

            Residue.LOGGER.info("[Residue] ObserverDataStore loaded ({} observers)",
                    assignments.size());

        } catch (Exception e) {
            Residue.LOGGER.warn("[Residue] Failed to load observer_data.json: {}",
                    e.getMessage());
        }
    }
}