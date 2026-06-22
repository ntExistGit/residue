package com.upphorattexistera.residue.observer;

import com.upphorattexistera.residue.config.ResidueConfig;

/**
 * Хелпер для получения настроек дистанции обсервера для конкретного стейджа памяти.
 * Инкапсулирует доступ к 20 полям ResidueConfig (5 стейджей x 4 параметра),
 * чтобы не дублировать switch-логику в нескольких местах.
 */
public record ObserverStageConfig(
        int minSpawnDistance,
        int maxSpawnDistance,
        int watchDistance,
        int criticalDistance
) {

    /**
     * Возвращает конфигурацию дистанций для указанного стейджа (0-4).
     * Значения вне диапазона зажимаются к границам.
     */
    public static ObserverStageConfig forStage(int stage) {
        ResidueConfig cfg = ResidueConfig.INSTANCE;

        return switch (Math.max(0, Math.min(4, stage))) {
            case 0 -> new ObserverStageConfig(
                    cfg.observerStage0MinSpawnDistance,
                    cfg.observerStage0MaxSpawnDistance,
                    cfg.observerStage0WatchDistance,
                    cfg.observerStage0CriticalDistance);
            case 1 -> new ObserverStageConfig(
                    cfg.observerStage1MinSpawnDistance,
                    cfg.observerStage1MaxSpawnDistance,
                    cfg.observerStage1WatchDistance,
                    cfg.observerStage1CriticalDistance);
            case 2 -> new ObserverStageConfig(
                    cfg.observerStage2MinSpawnDistance,
                    cfg.observerStage2MaxSpawnDistance,
                    cfg.observerStage2WatchDistance,
                    cfg.observerStage2CriticalDistance);
            case 3 -> new ObserverStageConfig(
                    cfg.observerStage3MinSpawnDistance,
                    cfg.observerStage3MaxSpawnDistance,
                    cfg.observerStage3WatchDistance,
                    cfg.observerStage3CriticalDistance);
            default -> new ObserverStageConfig(
                    cfg.observerStage4MinSpawnDistance,
                    cfg.observerStage4MaxSpawnDistance,
                    cfg.observerStage4WatchDistance,
                    cfg.observerStage4CriticalDistance);
        };
    }

    public boolean watchEnabled() {
        return watchDistance > 0;
    }

    public boolean criticalEnabled() {
        return criticalDistance > 0;
    }
}