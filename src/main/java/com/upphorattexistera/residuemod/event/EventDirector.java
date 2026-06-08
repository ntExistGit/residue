package com.upphorattexistera.residuemod.event;

import com.upphorattexistera.residuemod.WorldState;
import com.upphorattexistera.residuemod.event.events.DistantTorchEvent;
import net.minecraft.server.MinecraftServer;

public class EventDirector {

    public static void tick(MinecraftServer server) {

        int memory = WorldState.memory;

        if (WorldState.activeObserver == null) return;

        // stage 1
        if (memory > 20 && memory < 40) {
            tryJoinEcho(server);
        }

        // stage 2
        if (memory >= 40 && memory < 60) {
            tryDistantTorch(server);
        }

        // stage 3
        if (memory >= 60) {
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