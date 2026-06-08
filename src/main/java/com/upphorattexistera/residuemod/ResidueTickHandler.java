package com.upphorattexistera.residuemod;

import com.upphorattexistera.residuemod.event.EventDirector;
import com.upphorattexistera.residuemod.memory.MemoryManager;
import com.upphorattexistera.residuemod.observer.ObserverManager;
import net.minecraft.server.MinecraftServer;

public class ResidueTickHandler {

    public static void tick(MinecraftServer server) {

        WorldState.ticks++;

        // 1. memory update
        MemoryManager.tick(server);

        // 2. ensure observer exists
        ObserverManager.tick(server);

        // 3. events
        EventDirector.tick(server);
    }
}