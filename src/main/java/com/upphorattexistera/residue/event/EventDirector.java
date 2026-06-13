package com.upphorattexistera.residue.event;

import com.upphorattexistera.residue.config.ResidueConfig;
import com.upphorattexistera.residue.memory.MemoryManager;
import com.upphorattexistera.residue.observer.ObserverSessionManager;
import com.upphorattexistera.residue.event.events.DistantTorchEvent;
import com.upphorattexistera.residue.event.events.SelfCloneEvent;
import net.minecraft.server.MinecraftServer;

public class EventDirector {

    public static void tick(MinecraftServer server) {

        if (!ObserverSessionManager.hasObserver()) return;

        int memory = MemoryManager.getMemory();
        int max = ResidueConfig.INSTANCE.maxMemory;

        float percent = (float) memory / max * 100f;

        if (percent >= ResidueConfig.INSTANCE.torchMemoryMin
                && percent <= ResidueConfig.INSTANCE.torchMemoryMax) {
            DistantTorchEvent.tick(server);
        }

        if (percent >= ResidueConfig.INSTANCE.cloneMemoryMin
                && percent <= ResidueConfig.INSTANCE.cloneMemoryMax) {
            SelfCloneEvent.tick(server);
        }

        if (percent >= ResidueConfig.INSTANCE.dreamMemoryMin
                && percent <= ResidueConfig.INSTANCE.dreamMemoryMax) {
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