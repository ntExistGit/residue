package com.upphorattexistera.residuemod.memory;

import com.upphorattexistera.residuemod.WorldState;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryManager {

    private static final ConcurrentHashMap<UUID, Integer> playerMemory = new ConcurrentHashMap<>();

    private static int globalMemory = 0;

    public static void tick(MinecraftServer server) {

        globalMemory++;

        int online = server.getPlayerManager().getCurrentPlayerCount();

        if (online == 1) {
            globalMemory += 1;
        } else if (online > 1) {
            globalMemory += 2;
        }

        if (globalMemory > 1000) {
            globalMemory = 1000;
        }

        WorldState.memory = globalMemory;
    }

    public static int getMemory() {
        return globalMemory;
    }

    public static int getAttention() {
        return 0;
    }

    public static void addMemory(int amount) {
        globalMemory += amount;

        if (globalMemory < 0) globalMemory = 0;
        if (globalMemory > 1000) globalMemory = 1000;

        WorldState.memory = globalMemory;
    }

    public static void reset() {
        globalMemory = 0;
        playerMemory.clear();
        WorldState.memory = 0;
    }
}
