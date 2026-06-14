package com.upphorattexistera.residue;

import com.upphorattexistera.residue.command.ResidueCommands;
import com.upphorattexistera.residue.config.ResidueConfigSerializer;
import com.upphorattexistera.residue.entity.ObserverEntity;
import com.upphorattexistera.residue.entity.ObserverEntityType;
import com.upphorattexistera.residue.event.events.FakeLanEvent;
import com.upphorattexistera.residue.memory.MemoryManager;
import com.upphorattexistera.residue.network.FakeLanPacket;
import com.upphorattexistera.residue.network.ObserverListPacket;
import com.upphorattexistera.residue.network.ObserverMessagePacket;
import com.upphorattexistera.residue.network.ObserverMessageRequestPacket;
import com.upphorattexistera.residue.observer.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Residue implements ModInitializer {

    public static final String MOD_ID = "residue";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        ObserverEntityType.register();
        FabricDefaultAttributeRegistry.register(
                ObserverEntityType.OBSERVER,
                ObserverEntity.createAttributes()
        );

        ResidueConfigSerializer.load();

        FakeLanPacket.register();
        ObserverListPacket.register();
        ObserverMessagePacket.register();
        ObserverMessageRequestPacket.register();

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
            ObserverEntitySpawner.clearAll();
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