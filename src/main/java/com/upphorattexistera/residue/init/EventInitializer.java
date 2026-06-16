package com.upphorattexistera.residue.init;

import com.upphorattexistera.residue.Residue;
import com.upphorattexistera.residue.ResidueTickHandler;
import com.upphorattexistera.residue.event.events.FakeLanEvent;
import com.upphorattexistera.residue.memory.MemoryManager;
import com.upphorattexistera.residue.observer.*;
import com.upphorattexistera.residue.observer.persona.ObserverDataStore;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;

public class EventInitializer {
    public static void init() {

        // Тики
        ServerTickEvents.END_SERVER_TICK.register(ResidueTickHandler::tick);

        // Жизненный цикл сервера
        ServerLifecycleEvents.SERVER_STARTED.register(EventInitializer::onServerStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(EventInitializer::onServerStop);

        // Подключение игроков
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ObserverTabListManager.sendAllActive(handler.player);
        });
    }

    private static void onServerStart(MinecraftServer server) {
        ObserverManager.setDatabase(ObserverDataLoader.load(server));
        MemoryManager.onServerStarted(server);
        ObserverDataStore.onServerStarted(server);
        FakeLanEvent.init();
    }

    private static void onServerStop(MinecraftServer server) {
        MemoryManager.onServerStopping();
        ObserverDataStore.onServerStopping();

        ObserverConnectionEvent.reset();
        ObserverSessionManager.clear();
        ObserverSkinResolver.clearCache();
        ObserverEntitySpawner.clearAll();
        ObserverProactiveChat.reset();
    }
}