package com.upphorattexistera.residue.config;

import java.util.ArrayList;
import java.util.List;

public class ResidueConfig {

    public static final ResidueConfig INSTANCE = new ResidueConfig();

    // General

    public boolean enableMod = true;
    public boolean debugMode = false;

    public boolean llmEnable = true;
    public LLMBackend llmBackend = LLMBackend.AUTO;
    public LLMModel llmModel = LLMModel.QWEN_3B;
    public String customModelName = "";
    public LLMLanguage llmLang = LLMLanguage.ENGLISH;
    public boolean llmThink = false;
    public String huggingFaceToken = "";
    public int maxHistorySize = 40;

    // Twitch
    public boolean enableTwitchIntegration = false;
    public String twitchChannel = "";

    // Memory

    public int memoryIncreaseSeconds = 60;
    public int maxMemory = 1000;

    // Observer

    public boolean observerSystemEnabled = true;
    public int observerJoinMinMinutes = 10;
    public int observerJoinMaxMinutes = 40;

    // Events

    public boolean enableDreamEvent = true;
    public boolean enableDistantTorchEvent = true;
    public boolean enableSelfCloneEvent = true;

    // Voice Chat

    public boolean enableVoiceChatIntegration = true;
    public double voiceAttentionMultiplier = 1.0D;

    // Fake LAN

    public boolean enableFakeLanEvent = true;
    public int fakeLanMinMinutes = 5;
    public int fakeLanMaxMinutes = 20;

    // Observer connection
    public int observerConnectChanceLan = 50;
    public int observerConnectChanceNoLan = 1;
    public double observerFlapChance = 20.0;
    public int observerSessionMinSeconds = 120;
    public int observerSessionMaxSeconds = 600;
    public int observerMaxSimultaneous = 2;

    // Distant Torch
    public int torchMinDistance = 80;
    public int torchMaxDistance = 200;
    public int torchDespawnSeconds = 30;
    public double torchDisappearDistance = 6.0;
    public int torchMaxActive = 3;
    public int torchSpawnChance = 5;
    public int torchMemoryMin = 40;
    public int torchMemoryMax = 60;

    // Self Clone
    public int selfCloneMinDistance = 40;
    public int selfCloneMaxDistance = 80;
    public int selfCloneCooldownSeconds = 300;
    public int cloneMemoryMin = 60;
    public int cloneMemoryMax = 80;

    // Dream
    public int dreamMemoryMin = 80;
    public int dreamMemoryMax = 100;

    // Stage 0
    public int observerStage0MinSpawnDistance = 30;
    public int observerStage0MaxSpawnDistance = 70;
    public int observerStage0WatchDistance = 0;
    public int observerStage0CriticalDistance = 0;

    // Stage 1
    public int observerStage1MinSpawnDistance = 30;
    public int observerStage1MaxSpawnDistance = 70;
    public int observerStage1WatchDistance = 12;
    public int observerStage1CriticalDistance = 4;

    // Stage 2
    public int observerStage2MinSpawnDistance = 25;
    public int observerStage2MaxSpawnDistance = 60;
    public int observerStage2WatchDistance = 14;
    public int observerStage2CriticalDistance = 5;

    // Stage 3
    public int observerStage3MinSpawnDistance = 20;
    public int observerStage3MaxSpawnDistance = 50;
    public int observerStage3WatchDistance = 16;
    public int observerStage3CriticalDistance = 6;

    // Stage 4
    public int observerStage4MinSpawnDistance = 15;
    public int observerStage4MaxSpawnDistance = 40;
    public int observerStage4WatchDistance = 20;
    public int observerStage4CriticalDistance = 8;

    public float observerRaycastAngleDegrees = 120.0f;
    public List<String> observerRaycastIgnoreBlocks = RaycastIgnore.getAllRawIds();
}