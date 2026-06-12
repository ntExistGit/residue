package com.upphorattexistera.residuemod.observer;

import com.upphorattexistera.residuemod.Residue;
import com.upphorattexistera.residuemod.WorldState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Random;

public class ObserverManager {

    private static ObserverDatabase database;
    private static final Random RANDOM = new Random();

    public static void setDatabase(ObserverDatabase db) {
        database = db;
    }

    public static void tick(MinecraftServer server) {
        if (database == null) return;
        ObserverConnectionEvent.tick(server);
    }

    //public static void assignObserver(MinecraftServer server) {
    //    Observer observer = pickByWeight();
    //    if (observer == null) return;
    //    observer.setUsed(true);
    //    ObserverSessionManager.assignObserver(observer, WorldState.ticks);
    //    server.getPlayerManager().broadcast(
    //            Text.translatable(
    //                    "multiplayer.player.joined",
    //                    observer.getName()
    //            ).formatted(Formatting.YELLOW),
    //            false
    //    );
    //}

    public static void clearObserver() {
        ObserverSessionManager.clear();
    }

    public static List<Observer> getAll() {
        return database == null ? List.of() : database.observers;
    }

    public static Observer getStrongestUnusedObserver() {

        if (database == null) return null;

        return database.observers.stream()
                .filter(o -> !o.isUsed())
                .max((a, b) -> Integer.compare(a.getWeight(), b.getWeight()))
                .orElse(null);
    }

    public static void markUsed(Observer observer) {
        if (observer != null) {
            observer.setUsed(true);
        }
    }

    /**
     * Взвешенный случайный выбор из неиспользованных наблюдателей.
     * Наблюдатели с большим weight выпадают чаще.
     */
    private static Observer pickByWeight() {

        if (database == null) return null;

        List<Observer> unused = database.observers.stream()
                .filter(o -> !o.isUsed())
                .toList();

        // все исчерпаны — сбрасываем и начинаем заново
        if (unused.isEmpty()) {
            Residue.LOGGER.info("[Residue] All observers used, resetting");
            database.observers.forEach(o -> o.setUsed(false));
            unused = database.observers.stream()
                    .filter(o -> !o.isUsed())
                    .toList();
        }

        if (unused.isEmpty()) return null;

        int totalWeight = unused.stream()
                .mapToInt(Observer::getWeight)
                .sum();

        if (totalWeight <= 0) return unused.get(0);

        int roll = RANDOM.nextInt(totalWeight);
        int cumulative = 0;

        for (Observer o : unused) {
            cumulative += o.getWeight();
            if (roll < cumulative) {
                return o;
            }
        }

        return unused.get(unused.size() - 1);
    }

    public static Observer pickUnused() {
        return pickByWeight();
    }
}