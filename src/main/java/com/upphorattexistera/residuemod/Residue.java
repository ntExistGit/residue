package com.upphorattexistera.residuemod;

import com.upphorattexistera.residuemod.command.ResidueCommands;
import com.upphorattexistera.residuemod.config.ResidueConfig;
import com.upphorattexistera.residuemod.config.ResidueConfigSerializer;
import com.upphorattexistera.residuemod.event.events.FakeLanEvent;
import com.upphorattexistera.residuemod.memory.MemoryManager;
import com.upphorattexistera.residuemod.network.FakeLanPacket;
import com.upphorattexistera.residuemod.observer.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class Residue implements ModInitializer {

    public static final String MOD_ID = "residue";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        ResidueConfigSerializer.load();

        FakeLanPacket.register();

        ServerTickEvents.END_SERVER_TICK.register(
                ResidueTickHandler::tick
        );

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ObserverManager.setDatabase(ObserverDataLoader.load(server));
            LOGGER.info("[Residue] loaded {} observers", ObserverManager.getAll().size());
            MemoryManager.onServerStarted(server);
            FakeLanEvent.init();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            MemoryManager.onServerStopping();
            ObserverConnectionEvent.reset();
            ObserverSessionManager.clear();
            ObserverSkinResolver.clearCache();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ObserverTabListManager.sendAllActive(handler.player);
        });

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        ResidueCommands.register(dispatcher)
        );

        LOGGER.info("[Residue] initialized");
    }
}