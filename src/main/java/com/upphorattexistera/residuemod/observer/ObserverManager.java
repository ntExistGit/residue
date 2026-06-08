package com.upphorattexistera.residuemod.observer;

import com.upphorattexistera.residuemod.WorldState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ObserverManager {

    private static ObserverDatabase database;

    public static void setDatabase(ObserverDatabase db) {
        database = db;
    }

    public static void tick(MinecraftServer server) {

        if (database == null) return;

        if (WorldState.activeObserver != null) return;

        assignObserver(server);
    }

    public static void assignObserver(MinecraftServer server) {

        Observer observer =
                database.observers.stream()
                        .filter(o -> !o.isUsed())
                        .findFirst()
                        .orElse(null);

        if (observer == null) return;

        observer.setUsed(true);
        WorldState.activeObserver = observer;

        server.getPlayerList().broadcastSystemMessage(
                Component.translatable(
                        "multiplayer.player.joined",
                        observer.getName()
                ),
                false
        );
    }

    public static void clearObserver() {
        WorldState.activeObserver = null;
    }

    public static List<Observer> getAll() {
        return database == null ? List.of() : database.observers;
    }

    public static Observer getStrongestUnusedObserver() {

        if (database == null) return null;

        return database.observers.stream()
                .filter(o -> !o.isUsed())
                .findFirst()
                .orElse(null);
    }

    public static void markUsed(Observer observer) {
        if (observer != null) {
            observer.setUsed(true);
        }
    }
}