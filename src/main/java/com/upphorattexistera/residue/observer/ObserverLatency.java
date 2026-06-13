package com.upphorattexistera.residue.observer;

import com.upphorattexistera.residue.WorldState;

import java.util.Random;

public class ObserverLatency {

    private static final Random RANDOM = new Random();

    /**
     * Возвращает латентность в ms для отображения иконки пинга.
     * -1 = иконка X (нет соединения) — для флап-сессий.
     */
    public static int calculate(ObserverSessionManager.Session session) {

        long now = WorldState.ticks;
        long age = now - session.joinedAtTick;
        long remaining = session.disconnectAtTick - now;
        long totalDuration = session.disconnectAtTick - session.joinedAtTick;

        // Флап — иконка X
        if (totalDuration < 20 * 5) {
            return -1;
        }

        // Только подключился (< 10 сек) — плохой сигнал
        if (age < 20 * 10) {
            return 400 + RANDOM.nextInt(200); // 400-600ms → 2 полоски
        }

        // Скоро отключится (< 15 сек до конца) — деградирует
        if (remaining < 20 * 15) {
            return 500 + RANDOM.nextInt(300); // 500-800ms → 1-2 полоски
        }

        // Стабильная сессия — нормальный пинг
        return 80 + RANDOM.nextInt(120); // 80-200ms → 4-5 полосок
    }

    /**
     * Иконка X в Minecraft = латентность -1.
     * Minecraft рендерит иконки так:
     * -1   → X (нет соединения)
     * 0-150   → 5 полосок
     * 150-300 → 4 полоски
     * 300-600 → 3 полоски
     * 600-999 → 2 полоски
     * 1000+   → 1 полоска
     */
    public static int toIconLevel(int latency) {
        if (latency < 0)   return -1;
        if (latency < 150) return 0;
        if (latency < 300) return 1;
        if (latency < 600) return 2;
        if (latency < 1000) return 3;
        return 4;
    }
}