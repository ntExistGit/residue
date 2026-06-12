package com.upphorattexistera.residuemod.config;

public class ResidueConfig {

    public static final ResidueConfig INSTANCE = new ResidueConfig();

    // General

    public boolean enableMod = true;
    public boolean debugMode = false;

    public boolean llmEnable = true;
    public LLMBackend llmBackend = LLMBackend.AUTO;
    public LLMModel llmModel = LLMModel.QWEN_3B;
    public String customModelName = "";
    public String huggingFaceToken = "";

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

    // Twitch

    public boolean enableTwitchIntegration = false;
    public String twitchChannel = "";

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
}