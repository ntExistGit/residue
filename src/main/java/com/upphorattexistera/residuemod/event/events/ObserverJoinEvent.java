package com.upphorattexistera.residuemod.event.events;

import com.upphorattexistera.residuemod.observer.Observer;
import com.upphorattexistera.residuemod.observer.ObserverManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ObserverJoinEvent {

    public static void trigger(MinecraftServer server) {

        Observer observer = ObserverManager.getStrongestUnusedObserver();

        if (observer == null) {
            return;
        }

        server.getPlayerManager().broadcast(
                Text.translatable(
                        "multiplayer.player.joined",
                        observer.getName()
                ).formatted(Formatting.YELLOW),
                false
        );

        ObserverManager.markUsed(observer);
    }
}
