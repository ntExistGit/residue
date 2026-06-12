package com.upphorattexistera.residuemod.observer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ObserverSessionManager {

    public static class Session {

        public final Observer observer;
        public final long joinedAtTick;
        public long disconnectAtTick;
        public int attention;

        public Session(Observer observer, long joinedAtTick, long disconnectAtTick) {
            this.observer = observer;
            this.joinedAtTick = joinedAtTick;
            this.disconnectAtTick = disconnectAtTick;
            this.attention = 0;
        }
    }

    private static final List<Session> activeSessions = new ArrayList<>();

    // ----------------------------------------------------------------
    // Управление сессиями
    // ----------------------------------------------------------------

    public static void addSession(Observer observer, long currentTick, long disconnectAtTick) {
        activeSessions.add(new Session(observer, currentTick, disconnectAtTick));
    }

    public static void removeSession(Observer observer) {
        activeSessions.removeIf(s -> s.observer == observer);
    }

    public static void clear() {
        activeSessions.clear();
    }

    // ----------------------------------------------------------------
    // Запросы состояния
    // ----------------------------------------------------------------

    public static boolean hasObserver() {
        return !activeSessions.isEmpty();
    }

    public static boolean isActive(Observer observer) {
        return activeSessions.stream().anyMatch(s -> s.observer == observer);
    }

    public static List<Session> getSessions() {
        return Collections.unmodifiableList(activeSessions);
    }

    // обратная совместимость для EventDirector и ResidueDebugHud
    public static Optional<Observer> getObserver() {
        return activeSessions.isEmpty()
                ? Optional.empty()
                : Optional.of(activeSessions.get(0).observer);
    }

    public static long getObserverAge(long currentTick) {
        if (activeSessions.isEmpty()) return 0L;
        return currentTick - activeSessions.get(0).joinedAtTick;
    }

    // ----------------------------------------------------------------
    // Attention
    // ----------------------------------------------------------------

    public static void addAttention(int amount) {
        for (Session s : activeSessions) {
            s.attention += amount;
        }
    }

    public static int getAttention() {
        return activeSessions.stream()
                .mapToInt(s -> s.attention)
                .sum();
    }
}