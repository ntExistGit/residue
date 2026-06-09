package com.upphorattexistera.residuemod.memory;

import com.upphorattexistera.residuemod.WorldState;
import com.upphorattexistera.residuemod.config.ResidueConfig;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoryManager {

    private static final ConcurrentHashMap<UUID, Integer> playerMemory = new ConcurrentHashMap<>();

    private static final AtomicInteger globalMemory = new AtomicInteger(0);

    public static void tick(MinecraftServer server) {

        int max = ResidueConfig.INSTANCE.maxMemory;

        int delta = 1;

        int online = server.getPlayerManager().getCurrentPlayerCount();

        if (online == 1) {
            delta += 1;
        } else if (online > 1) {
            delta += 2;
        }

        int updated = globalMemory.updateAndGet(current -> Math.min(current + delta, max));

        WorldState.memory = updated;
    }

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
}