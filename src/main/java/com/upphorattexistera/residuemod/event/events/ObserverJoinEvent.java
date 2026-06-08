package com.upphorattexistera.residuemod.event.events;

import com.upphorattexistera.residuemod.observer.Observer;
import com.upphorattexistera.residuemod.observer.ObserverManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class ObserverJoinEvent {

    public static void trigger(MinecraftServer server) {

        Observer observer = ObserverManager.getStrongestUnusedObserver();

        if (observer == null) {
            return;
        }

        server.getPlayerList().broadcastSystemMessage(
                Component.translatable(
                        "multiplayer.player.joined",
                        observer.getName()
                ).withStyle(ChatFormatting.YELLOW),
                false
        );

        ObserverManager.markUsed(observer);
    }
}