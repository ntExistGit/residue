package com.upphorattexistera.residue.observer.persona;

import java.util.Map;

public class ObserverType {

    public final String id;
    public final int cooldownMinTicks;
    public final int cooldownMaxTicks;
    public final Map<Integer, Double> chancePerStage;
    public final Map<String, String> contexts;

    private static final int TICKS_PER_SECOND = 20;

    public ObserverType(String id, int cooldownMinSeconds, int cooldownMaxSeconds,
                        Map<Integer, Double> chancePerStage, Map<String, String> contexts) {
        this.id = id;
        this.cooldownMinTicks = cooldownMinSeconds * TICKS_PER_SECOND;
        this.cooldownMaxTicks = cooldownMaxSeconds * TICKS_PER_SECOND;
        this.chancePerStage = chancePerStage;
        this.contexts = contexts;
    }

    public double getChance(int stage) {
        return chancePerStage.getOrDefault(stage, 0.0);
    }

    /**
     * Возвращает случайный кулдаун между min и max.
     */
    public int getRandomCooldownTicks(java.util.Random random) {
        if (cooldownMaxTicks <= cooldownMinTicks) return cooldownMinTicks;
        return cooldownMinTicks + random.nextInt(cooldownMaxTicks - cooldownMinTicks);
    }

    /**
     * Возвращает контекст по ключу, fallback на "default".
     */
    public String getContext(String key) {
        return contexts.getOrDefault(key, contexts.getOrDefault("default", ""));
    }
}