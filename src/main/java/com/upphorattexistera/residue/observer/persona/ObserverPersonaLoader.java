package com.upphorattexistera.residue.observer.persona;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.upphorattexistera.residue.Residue;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ObserverPersonaLoader {

    private static final List<ObserverPersona> personas = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static String globalRules = "";

    public static void load() {
        personas.clear();

        try {
            // Сначала пробуем загрузить из папки конфига (для кастомизации)
            Path external = FabricLoader.getInstance().getConfigDir()
                    .resolve("residue/observer_personas.json");

            InputStream stream;
            if (Files.exists(external)) {
                stream = Files.newInputStream(external);
                Residue.LOGGER.info("[Residue] Loaded observer_personas.json from config");
            } else {
                // Загружаем из ресурсов мода
                Optional<ModContainer> mod = FabricLoader.getInstance()
                        .getModContainer("residue");
                if (mod.isEmpty()) return;

                Optional<Path> path = mod.get()
                        .findPath("assets/residue/observer_personas.json");
                if (path.isEmpty()) {
                    Residue.LOGGER.warn("[Residue] observer_personas.json not found in assets");
                    return;
                }
                stream = Files.newInputStream(path.get());
                Residue.LOGGER.info("[Residue] Loaded observer_personas.json from assets");
            }

            JsonObject root = JsonParser.parseReader(
                            new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();

            for (JsonElement elem : root.getAsJsonArray("personas")) {
                JsonObject obj = elem.getAsJsonObject();

                int id = obj.get("id").getAsInt();
                String gender = obj.get("gender").getAsString();
                double temperature = obj.get("temperature").getAsDouble();
                int maxTokens = obj.get("max_tokens").getAsInt();

                Map<Integer, List<String>> stages = new LinkedHashMap<>();
                JsonObject stagesObj = obj.getAsJsonObject("stages");

                for (Map.Entry<String, JsonElement> entry : stagesObj.entrySet()) {
                    int stageNum = Integer.parseInt(entry.getKey());
                    List<String> lines = new ArrayList<>();
                    for (JsonElement line : entry.getValue().getAsJsonArray()) {
                        lines.add(line.getAsString());
                    }
                    stages.put(stageNum, lines);
                }

                String typeStr = obj.has("type") ? obj.get("type").getAsString() : "";
                Set<String> types = new HashSet<>();
                for (String t : typeStr.split(",")) {
                    String trimmed = t.trim();
                    if (!trimmed.isEmpty()) types.add(trimmed);
                }

                personas.add(new ObserverPersona(id, gender, types, temperature, maxTokens, stages));

                Residue.LOGGER.debug("[Residue] Loaded persona id={} gender={}",
                        id, gender);
            }

            Residue.LOGGER.info("[Residue] Loaded {} observer personas", personas.size());

            if (root.has("global_rules")) {
                List<String> rules = new ArrayList<>();
                for (JsonElement rule : root.getAsJsonArray("global_rules")) {
                    rules.add(rule.getAsString());
                }
                globalRules = String.join("\n", rules);
            }

        } catch (Exception e) {
            Residue.LOGGER.error("[Residue] Failed to load observer_personas.json: {}",
                    e.getMessage());
        }
    }

    public static String getGlobalRules() {
        return globalRules;
    }

    public static ObserverPersona getById(int id) {
        return personas.stream()
                .filter(p -> p.id == id)
                .findFirst()
                .orElse(personas.isEmpty() ? null : personas.getFirst());
    }

    public static ObserverPersona getRandom() {
        if (personas.isEmpty()) return null;
        return personas.get(RANDOM.nextInt(personas.size()));
    }
}