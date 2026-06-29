package com.upphorattexistera.residue.observer.persona;

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

public class ObserverTypeLoader {

    private static final Map<String, ObserverType> types = new LinkedHashMap<>();
    private static final Random RANDOM = new Random();

    public static void load() {
        types.clear();

        try {
            Path external = FabricLoader.getInstance().getConfigDir()
                    .resolve("residue/observer_types.json");

            InputStream stream;
            if (Files.exists(external)) {
                stream = Files.newInputStream(external);
                Residue.LOGGER.info("[residue] Loaded observer_types.json from config");
            } else {
                Optional<ModContainer> mod = FabricLoader.getInstance()
                        .getModContainer("residue");
                if (mod.isEmpty()) return;

                Optional<Path> path = mod.get()
                        .findPath("assets/residue/observer_types.json");
                if (path.isEmpty()) {
                    Residue.LOGGER.warn("[residue] observer_types.json not found in assets");
                    return;
                }
                stream = Files.newInputStream(path.get());
                Residue.LOGGER.info("[residue] Loaded observer_types.json from assets");
            }

            JsonObject root = JsonParser.parseReader(
                            new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();

            for (JsonElement elem : root.getAsJsonArray("types")) {
                JsonObject obj = elem.getAsJsonObject();

                String id = obj.get("id").getAsString();
                int cooldownMin = obj.get("cooldown_min_seconds").getAsInt();
                int cooldownMax = obj.get("cooldown_max_seconds").getAsInt();

                Map<Integer, Double> chancePerStage = new LinkedHashMap<>();
                JsonObject chanceObj = obj.getAsJsonObject("chance_per_stage");
                for (Map.Entry<String, JsonElement> entry : chanceObj.entrySet()) {
                    chancePerStage.put(
                            Integer.parseInt(entry.getKey()),
                            entry.getValue().getAsDouble());
                }

                Map<String, String> contexts = new LinkedHashMap<>();
                JsonObject contextsObj = obj.getAsJsonObject("contexts");
                for (Map.Entry<String, JsonElement> entry : contextsObj.entrySet()) {
                    contexts.put(entry.getKey(), entry.getValue().getAsString());
                }

                types.put(id, new ObserverType(id, cooldownMin, cooldownMax,
                        chancePerStage, contexts));

                Residue.LOGGER.debug("[residue] Loaded observer type: {}", id);
            }

            Residue.LOGGER.info("[residue] Loaded {} observer types", types.size());

        } catch (Exception e) {
            Residue.LOGGER.error("[residue] Failed to load observer_types.json: {}",
                    e.getMessage());
        }
    }

    public static ObserverType getById(String id) {
        return types.get(id);
    }

    public static Collection<ObserverType> getAll() {
        return Collections.unmodifiableCollection(types.values());
    }
}