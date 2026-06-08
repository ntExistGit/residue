package com.upphorattexistera.residuemod.observer;

import net.minecraft.server.MinecraftServer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ObserverManager {

    private static ObserverDatabase database;

    public static void load(MinecraftServer server) {

        database =
                ObserverDataLoader.load(server);
    }

    public static List<Observer> getAll() {

        return database.observers;
    }

    public static Optional<Observer> getStrongestUnusedObserver() {

        return database.observers
                .stream()
                .filter(observer -> !observer.isUsed())
                .max(
                        Comparator.comparingInt(
                                Observer::getWeight
                        )
                );
    }

    public static void markUsed(
            Observer observer
    ) {

        observer.setUsed(true);
    }
}