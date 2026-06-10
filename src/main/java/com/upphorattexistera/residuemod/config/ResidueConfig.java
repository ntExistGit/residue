package com.upphorattexistera.residuemod.config;

public class ResidueConfig {

    public static final ResidueConfig INSTANCE = new ResidueConfig();

    // General

    public boolean enableMod = true;
    public boolean debugMode = false;

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

    public boolean enableFakeLanOpen = true;

    // Distant Torch
    public int torchMinDistance = 80;
    public int torchMaxDistance = 200;
    public int torchDespawnSeconds = 30;
    public double torchDisappearDistance = 6.0;
    public int torchMaxActive = 3;
    public int torchSpawnChance = 5;

    // Self Clone
    public int selfCloneMinDistance = 40;
    public int selfCloneMaxDistance = 80;
    public int selfCloneCooldownSeconds = 300;
}