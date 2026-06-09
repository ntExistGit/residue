package com.upphorattexistera.residuemod;

import com.upphorattexistera.residuemod.command.ResidueCommands;
import com.upphorattexistera.residuemod.config.ResidueConfigSerializer;
import com.upphorattexistera.residuemod.observer.ObserverDataLoader;
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

        ResidueConfigSerializer.load();

        ServerTickEvents.END_SERVER_TICK.register(
                ResidueTickHandler::tick
        );

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ObserverManager.setDatabase(ObserverDataLoader.load(server));
            LOGGER.info("[Residue] loaded {} observers", ObserverManager.getAll().size());
        });

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        ResidueCommands.register(dispatcher)
        );

        LOGGER.info("[Residue] initialized");
    }
}