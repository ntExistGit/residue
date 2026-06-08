package com.upphorattexistera.residuemod.event;

import java.util.HashMap;
import java.util.Map;

public class EventCooldownManager {

    private static final Map<String, Long> cooldowns = new HashMap<>();

    public static boolean canTrigger(String event, long currentTick, long cooldownTicks) {

        long last = cooldowns.getOrDefault(event, -999999L);

        if (currentTick - last < cooldownTicks) {
            return false;
        }

        cooldowns.put(event, currentTick);
        return true;
    }
}