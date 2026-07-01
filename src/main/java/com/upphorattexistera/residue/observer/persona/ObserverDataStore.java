package com.upphorattexistera.residue.observer.persona;

import com.google.gson.*;
import com.upphorattexistera.residue.Residue;
import com.upphorattexistera.residue.config.ResidueConfig;
import com.upphorattexistera.residue.config.TTSSpeakers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

    private static final Random RANDOM = new Random();

    public static ObserverAssignment getOrCreate(String observerName, String skinFile) {
        return assignments.computeIfAbsent(observerName, name -> {
            ObserverPersona persona = ObserverPersonaLoader.getRandom();
            int personaId = persona != null ? persona.id : 0;
            ObserverGender gender = ObserverGender.random(RANDOM);
            List<String> pool = TTSSpeakers.forGender(gender.filePrefix, ResidueConfig.INSTANCE.ttsEnabledSpeakers);
            if (pool.isEmpty()) pool = TTSSpeakers.ALL;

            ObserverAssignment assignment = new ObserverAssignment(name, personaId, skinFile, 0);

            assignment.gender = gender.filePrefix;
            assignment.ttsSpeaker = pool.get(RANDOM.nextInt(pool.size()));

            Residue.LOGGER.info("[Residue] New observer assignment: {} → persona={} gender={} speaker={}",
                    name, personaId, gender.filePrefix, assignment.ttsSpeaker);

            return assignment;
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
                obj.addProperty("gender", a.gender);
                obj.addProperty("tts_speaker", a.ttsSpeaker);
                obj.addProperty("skin_file", a.skinFile);
                obj.addProperty("skin_stage", a.skinStage);
                obj.add("conversation_history", a.conversationHistory);
                arr.add(obj);
            }

            root.add("observers", arr);

            Files.writeString(savePath,
                    new GsonBuilder().setPrettyPrinting().create().toJson(root),
                    StandardCharsets.UTF_8);

            Residue.LOGGER.debug("[residue] ObserverDataStore saved ({} entries)",
                    assignments.size());

        } catch (Exception e) {
            Residue.LOGGER.warn("[residue] Failed to save observer_data.json: {}",
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
                String gender = obj.has("gender")
                        ? obj.get("gender").getAsString()
                        : ObserverGender.random(RANDOM).filePrefix;
                String ttsSpeaker;
                if (obj.has("tts_speaker")) {
                    ttsSpeaker = obj.get("tts_speaker").getAsString();
                } else {
                    List<String> pool = TTSSpeakers.forGender(gender, ResidueConfig.INSTANCE.ttsEnabledSpeakers);
                    if (pool.isEmpty()) pool = TTSSpeakers.ALL;
                    ttsSpeaker = pool.get(RANDOM.nextInt(pool.size()));
                }
                String skinFile = obj.get("skin_file").getAsString();
                int skinStage = obj.get("skin_stage").getAsInt();

                ObserverAssignment assignment = new ObserverAssignment(name, personaId, skinFile, skinStage);
                assignment.gender = gender;
                assignment.ttsSpeaker = ttsSpeaker;

                if (obj.has("conversation_history")) {
                    assignment.conversationHistory =
                            obj.get("conversation_history").getAsJsonArray();
                }

                assignments.put(name, assignment);
                Residue.LOGGER.debug("[residue] Loaded observer: {} persona={}",
                        name, personaId);
            }

            Residue.LOGGER.info("[residue] ObserverDataStore loaded ({} observers)",
                    assignments.size());

        } catch (Exception e) {
            Residue.LOGGER.warn("[residue] Failed to load observer_data.json: {}",
                    e.getMessage());
        }
    }
}