package com.upphorattexistera.residue.memory;

import com.upphorattexistera.residue.config.ResidueConfig;

public class MemoryStage {

    private MemoryStage() {}

    public static int getStage(int memory, int max) {
        if (memory < max * 0.20) return 0;
        if (memory < max * 0.40) return 1;
        if (memory < max * 0.60) return 2;
        if (memory < max * 0.80) return 3;
        return 4;
    }

    public static int getCurrentStage() {
        int memory = MemoryManager.getMemory();
        int max = ResidueConfig.INSTANCE.maxMemory;
        return getStage(memory, max);
    }
}