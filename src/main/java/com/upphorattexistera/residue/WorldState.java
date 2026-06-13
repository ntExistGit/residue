package com.upphorattexistera.residue;

public class WorldState {

    public static volatile int memory = 0;

    public static volatile long ticks = 0;

    public static void reset() {
        memory = 0;
        ticks = 0;
    }
}