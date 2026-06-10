package com.upphorattexistera.residuemod.event;

import com.upphorattexistera.residuemod.config.ResidueConfig;
import com.upphorattexistera.residuemod.memory.MemoryManager;
import com.upphorattexistera.residuemod.observer.ObserverSessionManager;
import com.upphorattexistera.residuemod.event.events.DistantTorchEvent;
import com.upphorattexistera.residuemod.event.events.SelfCloneEvent;
import net.minecraft.server.MinecraftServer;

public class EventDirector {

    public static void tick(MinecraftServer server) {

        if (!ObserverSessionManager.hasObserver()) return;

        int memory = MemoryManager.getMemory();
        int max = ResidueConfig.INSTANCE.maxMemory;

        int s1start = (int) (max * 0.20);
        int s2start = (int) (max * 0.40);
        int s3start = (int) (max * 0.60);
        int s4start = (int) (max * 0.80);

        if (memory > s1start && memory < s2start) {
            tryJoinEcho(server);
        }

        if (memory >= s2start && memory < s3start) {
            tryDistantTorch(server);
        }

        if (memory >= s3start && memory < s4start) {
            SelfCloneEvent.tick(server);
        }

        if (memory >= s4start) {
            tryDreamGlitch(server);
        }
    }

    private static void tryJoinEcho(MinecraftServer server) {
        // placeholder
    }

    private static void tryDistantTorch(MinecraftServer server) {
        DistantTorchEvent.tick(server);
    }

    private static void tryDreamGlitch(MinecraftServer server) {
        // placeholder
    }
}