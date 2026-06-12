package com.upphorattexistera.residuemod.observer;

import com.upphorattexistera.residuemod.Residue;
import com.upphorattexistera.residuemod.WorldState;
import com.upphorattexistera.residuemod.config.ResidueConfig;
import com.upphorattexistera.residuemod.event.events.FakeLanEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ObserverConnectionEvent {

    private static final int TICKS_PER_SECOND = 20;
    private static final Random RANDOM = new Random();

    // очередь запланированных событий подключения/отключения
    private static final List<ScheduledEvent> queue = new ArrayList<>();

    // тик следующей попытки спавна нового наблюдателя
    private static long nextSpawnAttemptTick = 0L;

    // ----------------------------------------------------------------

    private enum EventType { CONNECT, DISCONNECT }

    private static class ScheduledEvent {
        final EventType type;
        final Observer observer;
        final long atTick;
        final boolean isFinal;

        ScheduledEvent(EventType type, Observer observer, long atTick, boolean isFinal) {
            this.type = type;
            this.observer = observer;
            this.atTick = atTick;
            this.isFinal = isFinal;
        }
    }

    // ----------------------------------------------------------------
    // Tick
    // ----------------------------------------------------------------

    public static void tick(MinecraftServer server) {

        long now = WorldState.ticks;

        // 1. обрабатываем запланированные события
        List<ScheduledEvent> due = new ArrayList<>();
        for (ScheduledEvent e : queue) {
            if (now >= e.atTick) due.add(e);
        }
        queue.removeAll(due);

        for (ScheduledEvent e : due) {
            if (e.type == EventType.CONNECT) {
                executeConnect(server, e.observer, e.isFinal);
            } else {
                executeDisconnect(server, e.observer);
            }
        }

        // 2. проверяем истёкшие сессии
        List<ObserverSessionManager.Session> expired = new ArrayList<>();
        for (ObserverSessionManager.Session s : ObserverSessionManager.getSessions()) {
            if (now >= s.disconnectAtTick) {
                expired.add(s);
            }
        }
        for (ObserverSessionManager.Session s : expired) {
            scheduleDisconnect(server, s.observer, 0);
        }

        // 3. попытка спавна нового наблюдателя
        if (now >= nextSpawnAttemptTick) {
            trySpawnObserver(server);
            nextSpawnAttemptTick = now + TICKS_PER_SECOND * 5; // проверяем раз в 5 секунд
        }
    }

    // ----------------------------------------------------------------
    // Спавн наблюдателя
    // ----------------------------------------------------------------

    private static void trySpawnObserver(MinecraftServer server) {

        ResidueConfig cfg = ResidueConfig.INSTANCE;

        if (ObserverSessionManager.getSessions().size() >= cfg.observerMaxSimultaneous) return;

        boolean lanActive = FakeLanEvent.isActive();

        double chance = lanActive
                ? cfg.observerConnectChanceLan / 1000.0
                : cfg.observerConnectChanceNoLan / 100000.0;

        if (RANDOM.nextDouble() >= chance) return;

        Observer observer = ObserverManager.pickUnused();
        if (observer == null) return;

        boolean flap = RANDOM.nextDouble() < cfg.observerFlapChance / 100.0;

        if (flap) {
            scheduleFlapSequence(server, observer);
        } else {
            scheduleConnect(server, observer, 0);
        }
    }

    // ----------------------------------------------------------------
    // Флапающее подключение
    // ----------------------------------------------------------------

    private static void scheduleFlapSequence(MinecraftServer server, Observer observer) {

        long now = WorldState.ticks;
        long cursor = now;

        int flapCount = 2 + RANDOM.nextInt(3);

        for (int i = 0; i < flapCount; i++) {
            cursor += randomTicks(1, 3);
            queue.add(new ScheduledEvent(EventType.CONNECT, observer, cursor, false));

            cursor += randomTicks(1, 4);
            queue.add(new ScheduledEvent(EventType.DISCONNECT, observer, cursor, false));
        }

        cursor += randomTicks(1, 3);
        queue.add(new ScheduledEvent(EventType.CONNECT, observer, cursor, true));
    }

    // ----------------------------------------------------------------
    // Планирование одиночных событий
    // ----------------------------------------------------------------

    private static void scheduleConnect(MinecraftServer server, Observer observer, long delayTicks) {
        queue.add(new ScheduledEvent(EventType.CONNECT, observer, WorldState.ticks + delayTicks, true));
    }

    private static void scheduleDisconnect(MinecraftServer server, Observer observer, long delayTicks) {
        queue.add(new ScheduledEvent(EventType.DISCONNECT, observer, WorldState.ticks + delayTicks, false));
    }

    // ----------------------------------------------------------------
    // Выполнение событий
    // ----------------------------------------------------------------

    private static void executeConnect(MinecraftServer server, Observer observer, boolean isFinal) {

        ResidueConfig cfg = ResidueConfig.INSTANCE;

        int minSec = cfg.observerSessionMinSeconds;
        int maxSec = cfg.observerSessionMaxSeconds;
        int sessionSeconds = minSec + RANDOM.nextInt(Math.max(1, maxSec - minSec));
        long disconnectAt = WorldState.ticks + (long) sessionSeconds * TICKS_PER_SECOND;

        if (ObserverSessionManager.isActive(observer)) {
            for (ObserverSessionManager.Session s : ObserverSessionManager.getSessions()) {
                if (s.observer == observer) {
                    s.disconnectAtTick = disconnectAt;
                    return;
                }
            }
        }

        // помечаем used только при финальном подключении
        if (isFinal) {
            observer.setUsed(true);
        }

        ObserverSessionManager.addSession(observer, WorldState.ticks, disconnectAt);
        ObserverTabListManager.sendAdd(server, observer);

        // запускаем поиск скина
        ObserverSkinResolver.resolve(observer.getName()).thenAccept(skinData -> {
            if (skinData.hasTextures()) {
                Residue.LOGGER.debug("[Residue] Skin resolved for {} via {}",
                        observer.getName(), skinData.getSource());
                ObserverTabListManager.sendAdd(server, observer);
            }
        });

        server.getPlayerManager().broadcast(
                Text.translatable("multiplayer.player.joined", observer.getName())
                        .formatted(Formatting.YELLOW),
                false
        );
    }

    private static void executeDisconnect(MinecraftServer server, Observer observer) {

        if (!ObserverSessionManager.isActive(observer)) return;

        ObserverSessionManager.removeSession(observer);
        ObserverTabListManager.sendRemove(server, observer);

        server.getPlayerManager().broadcast(
                Text.translatable("multiplayer.player.left", observer.getName())
                        .formatted(Formatting.YELLOW),
                false
        );
    }

    // ----------------------------------------------------------------
    // Утилиты
    // ----------------------------------------------------------------

    private static long randomTicks(int minSeconds, int maxSeconds) {
        return (long) (minSeconds + RANDOM.nextInt(maxSeconds - minSeconds + 1)) * TICKS_PER_SECOND;
    }

    public static void reset() {
        queue.clear();
        nextSpawnAttemptTick = 0L;
    }

    public static void forceConnect(MinecraftServer server, Observer observer) {
        executeConnect(server, observer, true);
    }

    public static void forceDisconnect(MinecraftServer server, Observer observer) {
        executeDisconnect(server, observer);
    }
}