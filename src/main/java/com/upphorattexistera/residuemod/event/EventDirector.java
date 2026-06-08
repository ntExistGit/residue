package com.upphorattexistera.residuemod.event;

import com.upphorattexistera.residuemod.event.events.ObserverJoinEvent;
import com.upphorattexistera.residuemod.memory.MemoryManager;
import net.minecraft.server.MinecraftServer;

public class EventDirector {

    private static long ticks = 0;

    public static void tick(MinecraftServer server) {

        ticks++;

        if (ticks % 1200 != 0) return;

        MemoryManager.addMemory(1);

        if (MemoryManager.getMemory() == 5) {
            ObserverJoinEvent.trigger(server);
        }
    }
}