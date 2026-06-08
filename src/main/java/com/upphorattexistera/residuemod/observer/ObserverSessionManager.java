package com.upphorattexistera.residuemod.observer;

import java.util.Optional;

public class ObserverSessionManager {

    private static Observer activeObserver;

    private static long observerSince;

    private static int attention;

    public static void assignObserver(
            Observer observer,
            long currentTick
    ) {

        activeObserver = observer;
        observerSince = currentTick;
        attention = 0;
    }

    public static boolean hasObserver() {

        return activeObserver != null;
    }

    public static Optional<Observer> getObserver() {

        return Optional.ofNullable(
                activeObserver
        );
    }

    public static long getObserverAge(
            long currentTick
    ) {

        return currentTick - observerSince;
    }

    public static void addAttention(
            int amount
    ) {

        attention += amount;
    }

    public static int getAttention() {

        return attention;
    }

    public static void clear() {

        activeObserver = null;
        observerSince = 0;
        attention = 0;
    }
}