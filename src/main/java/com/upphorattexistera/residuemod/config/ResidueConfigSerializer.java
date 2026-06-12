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

        // General
        cfg.enableMod = loaded.enableMod;
        cfg.debugMode = loaded.debugMode;

        cfg.llmEnable = loaded.llmEnable;
        cfg.llmBackend  = loaded.llmBackend;
        cfg.llmModel = loaded.llmModel;
        cfg.customModelName = loaded.customModelName;
        cfg.huggingFaceToken = loaded.huggingFaceToken;

        // Memory
        cfg.memoryIncreaseSeconds = loaded.memoryIncreaseSeconds;
        cfg.maxMemory = loaded.maxMemory;

        // Observer
        cfg.observerSystemEnabled = loaded.observerSystemEnabled;
        cfg.observerJoinMinMinutes = loaded.observerJoinMinMinutes;
        cfg.observerJoinMaxMinutes = loaded.observerJoinMaxMinutes;

        // Events
        cfg.enableDreamEvent = loaded.enableDreamEvent;
        cfg.enableDistantTorchEvent = loaded.enableDistantTorchEvent;
        cfg.enableSelfCloneEvent = loaded.enableSelfCloneEvent;

        // Voice Chat
        cfg.enableVoiceChatIntegration = loaded.enableVoiceChatIntegration;
        cfg.voiceAttentionMultiplier = loaded.voiceAttentionMultiplier;

        // Twitch
        cfg.enableTwitchIntegration = loaded.enableTwitchIntegration;
        cfg.twitchChannel = loaded.twitchChannel;

        // Fake LAN
        cfg.enableFakeLanEvent = loaded.enableFakeLanEvent;
        cfg.fakeLanMinMinutes = loaded.fakeLanMinMinutes;
        cfg.fakeLanMaxMinutes = loaded.fakeLanMaxMinutes;

        // Observer connection
        cfg.observerConnectChanceLan = loaded.observerConnectChanceLan;
        cfg.observerConnectChanceNoLan = loaded.observerConnectChanceNoLan;
        cfg.observerFlapChance = loaded.observerFlapChance;
        cfg.observerSessionMinSeconds = loaded.observerSessionMinSeconds;
        cfg.observerSessionMaxSeconds = loaded.observerSessionMaxSeconds;
        cfg.observerMaxSimultaneous = loaded.observerMaxSimultaneous;

        // Distant Torch
        cfg.torchMinDistance = loaded.torchMinDistance;
        cfg.torchMaxDistance = loaded.torchMaxDistance;
        cfg.torchDespawnSeconds = loaded.torchDespawnSeconds;
        cfg.torchDisappearDistance = loaded.torchDisappearDistance;
        cfg.torchMaxActive = loaded.torchMaxActive;
        cfg.torchSpawnChance = loaded.torchSpawnChance;
        cfg.torchMemoryMin = loaded.torchMemoryMin;
        cfg.torchMemoryMax = loaded.torchMemoryMax;

        // Self Clone
        cfg.selfCloneMinDistance = loaded.selfCloneMinDistance;
        cfg.selfCloneMaxDistance = loaded.selfCloneMaxDistance;
        cfg.selfCloneCooldownSeconds = loaded.selfCloneCooldownSeconds;
        cfg.cloneMemoryMin = loaded.cloneMemoryMin;
        cfg.cloneMemoryMax = loaded.cloneMemoryMax;

        // Dream
        cfg.dreamMemoryMin = loaded.dreamMemoryMin;
        cfg.dreamMemoryMax = loaded.dreamMemoryMax;
    }
}