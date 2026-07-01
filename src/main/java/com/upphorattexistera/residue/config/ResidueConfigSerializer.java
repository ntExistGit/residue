package com.upphorattexistera.residue.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResidueConfigSerializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("residue.json");

    public static void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                save();
                return;
            }
            Reader reader = Files.newBufferedReader(CONFIG_PATH);
            ResidueConfig loaded = GSON.fromJson(reader, ResidueConfig.class);
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
            Writer writer = Files.newBufferedWriter(CONFIG_PATH);
            GSON.toJson(ResidueConfig.INSTANCE, writer);
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
        cfg.language = loaded.language != null ? loaded.language : Language.ENGLISH;

        // LLM
        cfg.llmEnable = loaded.llmEnable;
        cfg.llmBackend = loaded.llmBackend;
        cfg.llmModel = loaded.llmModel;
        cfg.customModelName = loaded.customModelName;
        cfg.llmThink = loaded.llmThink;
        cfg.huggingFaceToken = loaded.huggingFaceToken;
        cfg.maxHistorySize = loaded.maxHistorySize;

        // TTS
        cfg.ttsEnable = loaded.ttsEnable;
        cfg.ttsAudibleDistance = loaded.ttsAudibleDistance > 0 ? loaded.ttsAudibleDistance : 32.0;
        cfg.ttsPreRollMs = loaded.ttsPreRollMs >= 0 ? loaded.ttsPreRollMs : 300;
        cfg.ttsTokenizer = loaded.ttsTokenizer != null ? loaded.ttsTokenizer : TTSTokenizer.Q4_K_M;
        cfg.ttsTalker = loaded.ttsTalker != null ? loaded.ttsTalker : TTSTalker.QWEN_TALKER_1_7B_CUSTOM_Q4;
        cfg.ttsCustomTokenizerName = loaded.ttsCustomTokenizerName;
        cfg.ttsCustomTalkerName = loaded.ttsCustomTalkerName;
        cfg.ttsEnabledSpeakers = loaded.ttsEnabledSpeakers != null
                ? loaded.ttsEnabledSpeakers
                : new java.util.ArrayList<>(TTSSpeakers.ALL);

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

        // Observer Entity — per-stage distances
        cfg.observerStage0MinSpawnDistance = loaded.observerStage0MinSpawnDistance;
        cfg.observerStage0MaxSpawnDistance = loaded.observerStage0MaxSpawnDistance;
        cfg.observerStage0WatchDistance = loaded.observerStage0WatchDistance;
        cfg.observerStage0CriticalDistance = loaded.observerStage0CriticalDistance;

        cfg.observerStage1MinSpawnDistance = loaded.observerStage1MinSpawnDistance;
        cfg.observerStage1MaxSpawnDistance = loaded.observerStage1MaxSpawnDistance;
        cfg.observerStage1WatchDistance = loaded.observerStage1WatchDistance;
        cfg.observerStage1CriticalDistance = loaded.observerStage1CriticalDistance;

        cfg.observerStage2MinSpawnDistance = loaded.observerStage2MinSpawnDistance;
        cfg.observerStage2MaxSpawnDistance = loaded.observerStage2MaxSpawnDistance;
        cfg.observerStage2WatchDistance = loaded.observerStage2WatchDistance;
        cfg.observerStage2CriticalDistance = loaded.observerStage2CriticalDistance;

        cfg.observerStage3MinSpawnDistance = loaded.observerStage3MinSpawnDistance;
        cfg.observerStage3MaxSpawnDistance = loaded.observerStage3MaxSpawnDistance;
        cfg.observerStage3WatchDistance = loaded.observerStage3WatchDistance;
        cfg.observerStage3CriticalDistance = loaded.observerStage3CriticalDistance;

        cfg.observerStage4MinSpawnDistance = loaded.observerStage4MinSpawnDistance;
        cfg.observerStage4MaxSpawnDistance = loaded.observerStage4MaxSpawnDistance;
        cfg.observerStage4WatchDistance = loaded.observerStage4WatchDistance;
        cfg.observerStage4CriticalDistance = loaded.observerStage4CriticalDistance;

        cfg.observerRaycastAngleDegrees = loaded.observerRaycastAngleDegrees;
        cfg.observerRaycastIgnoreBlocks = loaded.observerRaycastIgnoreBlocks != null
                ? loaded.observerRaycastIgnoreBlocks
                : RaycastIgnore.getAllRawIds();

        // Observer Work
        cfg.enableObserverWork = loaded.enableObserverWork;
        cfg.observerWorkRadius = loaded.observerWorkRadius;
        cfg.observerWorkDurationTicks = loaded.observerWorkDurationTicks;
        cfg.observerWorkMaxBlocksPerSession = loaded.observerWorkMaxBlocksPerSession;
        cfg.observerWorkChancePerSecond = loaded.observerWorkChancePerSecond;
        cfg.observerWorkDropItems = loaded.observerWorkDropItems;
        cfg.observerCanPickUpLoot = loaded.observerCanPickUpLoot;
    }
}