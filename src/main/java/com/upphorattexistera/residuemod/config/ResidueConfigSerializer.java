package com.upphorattexistera.residuemod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResidueConfigSerializer {

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("residue.json");

    public static void load() {

        try {

            if (!Files.exists(CONFIG_PATH)) {
                save();
                return;
            }

            Reader reader = Files.newBufferedReader(CONFIG_PATH);

            ResidueConfig loaded =
                    GSON.fromJson(reader, ResidueConfig.class);

            reader.close();

            if (loaded == null) {
                save();
                return;
            }

            copy(loaded);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {

        try {

            Writer writer =
                    Files.newBufferedWriter(CONFIG_PATH);

            GSON.toJson(
                    ResidueConfig.INSTANCE,
                    writer
            );

            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void copy(ResidueConfig loaded) {

        ResidueConfig cfg = ResidueConfig.INSTANCE;

        cfg.enableMod = loaded.enableMod;
        cfg.debugMode = loaded.debugMode;

        cfg.memoryIncreaseSeconds = loaded.memoryIncreaseSeconds;
        cfg.maxMemory = loaded.maxMemory;

        cfg.observerSystemEnabled = loaded.observerSystemEnabled;
        cfg.observerJoinMinMinutes = loaded.observerJoinMinMinutes;
        cfg.observerJoinMaxMinutes = loaded.observerJoinMaxMinutes;

        cfg.enableDreamEvent = loaded.enableDreamEvent;
        cfg.enableDistantTorchEvent = loaded.enableDistantTorchEvent;
        cfg.enableSelfCloneEvent = loaded.enableSelfCloneEvent;

        cfg.enableVoiceChatIntegration = loaded.enableVoiceChatIntegration;
        cfg.voiceAttentionMultiplier = loaded.voiceAttentionMultiplier;

        cfg.enableTwitchIntegration = loaded.enableTwitchIntegration;
        cfg.twitchChannel = loaded.twitchChannel;

        cfg.enableFakeLanOpen = loaded.enableFakeLanOpen;
    }
}