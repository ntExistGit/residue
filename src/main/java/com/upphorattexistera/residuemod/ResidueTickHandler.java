package com.upphorattexistera.residuemod;

import com.upphorattexistera.residuemod.event.EventDirector;
import com.upphorattexistera.residuemod.event.events.FakeLanEvent;
import com.upphorattexistera.residuemod.memory.MemoryManager;
import com.upphorattexistera.residuemod.observer.ObserverManager;
import net.minecraft.server.MinecraftServer;

public class ResidueTickHandler {

    public static void tick(MinecraftServer server) {

        WorldState.ticks++;

        MemoryManager.tick(server);

        ObserverManager.tick(server);

        EventDirector.tick(server);

        FakeLanEvent.tick(server);
    }
}