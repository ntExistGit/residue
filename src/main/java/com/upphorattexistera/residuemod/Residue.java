package com.upphorattexistera.residuemod;

import com.upphorattexistera.residuemod.command.ResidueCommands;
import com.upphorattexistera.residuemod.event.EventDirector;
import com.upphorattexistera.residuemod.memory.MemoryManager;
import com.upphorattexistera.residuemod.observer.ObserverManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Residue implements ModInitializer {

    public static final String MOD_ID = "residue";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        LOGGER.info("Residue initializing...");

        MemoryManager.load();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {

            ObserverManager.load(server);

            LOGGER.info(
                    "Loaded observers: "
                            + ObserverManager.getAll().size()
            );
        });

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        ResidueCommands.register(dispatcher)
        );

        ServerTickEvents.END_SERVER_TICK.register(EventDirector::tick);

        LOGGER.info("Residue initialized");
    }
}