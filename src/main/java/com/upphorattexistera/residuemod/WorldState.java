package com.upphorattexistera.residuemod;

import com.upphorattexistera.residuemod.observer.Observer;

public class WorldState {

    public static int memory = 0;

    public static Observer activeObserver = null;

    public static long ticks = 0;

    public static void reset() {
        memory = 0;
        activeObserver = null;
        ticks = 0;
    }
}