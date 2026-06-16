package com.upphorattexistera.residue;

import com.upphorattexistera.residue.config.ResidueConfig;
import com.upphorattexistera.residue.event.EventDirector;
import com.upphorattexistera.residue.event.events.FakeLanEvent;
import com.upphorattexistera.residue.memory.MemoryManager;
import com.upphorattexistera.residue.observer.ObserverConnectionEvent;
import com.upphorattexistera.residue.observer.ObserverManager;
import com.upphorattexistera.residue.observer.ObserverProactiveChat;
import net.minecraft.server.MinecraftServer;

public class ResidueTickHandler {

    public static void tick(MinecraftServer server) {

        if (!ResidueConfig.INSTANCE.enableMod) return; // ← добавить

        WorldState.ticks++;

        MemoryManager.tick(server);
        ObserverManager.tick(server);
        EventDirector.tick(server);
        FakeLanEvent.tick(server);
        ObserverProactiveChat.tick(server);

        if (WorldState.ticks % 100 == 0) {
            ObserverConnectionEvent.broadcastObserverList(server);
        }
    }
}