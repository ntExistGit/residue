package com.upphorattexistera.residuemod.memory;

import com.upphorattexistera.residuemod.Residue;
import com.upphorattexistera.residuemod.WorldState;
import com.upphorattexistera.residuemod.config.ResidueConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoryManager {

    private static final ConcurrentHashMap<UUID, Integer> playerMemory = new ConcurrentHashMap<>();

    private static final AtomicInteger globalMemory = new AtomicInteger(0);

    public static void tick(MinecraftServer server) {

        int max = ResidueConfig.INSTANCE.maxMemory;
        int intervalTicks = ResidueConfig.INSTANCE.memoryIncreaseSeconds * TICKS_PER_SECOND;

        if (WorldState.ticks % intervalTicks != 0) return;

        int online = server.getPlayerManager().getCurrentPlayerCount();

        int delta = 1 + (online == 1 ? 1 : online > 1 ? 2 : 0);

        int updated = globalMemory.updateAndGet(current -> Math.min(current + delta, max));
        WorldState.memory = updated;
    }

    private static final int TICKS_PER_SECOND = 20;

    public static int getMemory() {
        return globalMemory.get();
    }

    public static int getAttention() {
        return 0;
    }

    public static void addMemory(int amount) {

        int max = ResidueConfig.INSTANCE.maxMemory;

        int updated = globalMemory.updateAndGet(current -> {
            int next = current + amount;
            if (next < 0) return 0;
            if (next > max) return max;
            return next;
        });

        WorldState.memory = updated;
    }

    public static void reset() {
        globalMemory.set(0);
        playerMemory.clear();
        WorldState.memory = 0;
    }

    private static Path savePath;

    public static void onServerStarted(MinecraftServer server) {
        savePath = server.getSavePath(WorldSavePath.ROOT).resolve("residue_memory.dat");
        load();
    }

    public static void onServerStopping() {
        save();
    }

    private static void load() {
        try {
            if (!Files.exists(savePath)) return;
            String raw = Files.readString(savePath).trim();
            int saved = Integer.parseInt(raw);
            int max = ResidueConfig.INSTANCE.maxMemory;
            globalMemory.set(Math.min(saved, max));
            WorldState.memory = globalMemory.get();
            Residue.LOGGER.info("[Residue] Memory loaded: {}", globalMemory.get());
        } catch (Exception e) {
            Residue.LOGGER.warn("[Residue] Failed to load memory: {}", e.getMessage());
        }
    }

    private static void save() {
        try {
            if (savePath == null) return;
            Files.writeString(savePath, String.valueOf(globalMemory.get()));
            Residue.LOGGER.info("[Residue] Memory saved: {}", globalMemory.get());
        } catch (Exception e) {
            Residue.LOGGER.warn("[Residue] Failed to save memory: {}", e.getMessage());
        }
    }
}