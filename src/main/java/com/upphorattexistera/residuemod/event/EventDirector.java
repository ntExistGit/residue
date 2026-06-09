package com.upphorattexistera.residuemod.event;

import com.upphorattexistera.residuemod.memory.MemoryManager;
import com.upphorattexistera.residuemod.observer.ObserverSessionManager;
import com.upphorattexistera.residuemod.event.events.DistantTorchEvent;
import com.upphorattexistera.residuemod.event.events.SelfCloneEvent;
import net.minecraft.server.MinecraftServer;

public class EventDirector {

    public static void tick(MinecraftServer server) {

        if (!ObserverSessionManager.hasObserver()) return;

        int memory = MemoryManager.getMemory();

        // stage 1: 20–40 — редкие сомнительные события
        if (memory > 20 && memory < 40) {
            tryJoinEcho(server);
        }

        // stage 2: 40–60 — наблюдатели начинают проявляться
        if (memory >= 40 && memory < 60) {
            tryDistantTorch(server);
        }

        // stage 3: 60–80 — явные нарушения реальности
        if (memory >= 60 && memory < 80) {
            SelfCloneEvent.tick(server);
        }

        // stage 4: 80–100 — критические события, сны, сильные аномалии
        if (memory >= 80) {
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