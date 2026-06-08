package com.upphorattexistera.residuemod.memory;

public class ResidueState {

    private static int memory;
    private static int attention;

    public static void addMemory(int amount) {
        memory += amount;
    }

    public static void addAttention(int amount) {
        attention += amount;
    }

    public static int getMemory() {
        return memory;
    }

    public static int getAttention() {
        return attention;
    }

    public static void reset() {
        memory = 0;
        attention = 0;
    }
}