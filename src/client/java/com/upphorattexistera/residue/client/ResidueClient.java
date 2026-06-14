package com.upphorattexistera.residue.client;

import com.upphorattexistera.residue.client.ai.ResidueClientCommands;
import com.upphorattexistera.residue.client.ai.ResidueClientConnectionEvents;
import com.upphorattexistera.residue.client.render.ObserverEntityRenderer;
import com.upphorattexistera.residue.entity.ObserverEntityType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.entity.EntityRendererFactories;

public class ResidueClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("sway")) {
            SwayCompatLoader.register();
        }
        ResidueDebugHud.register();
        ResidueClientEvents.register();

        EntityRendererFactories.register(
                ObserverEntityType.OBSERVER,
                ObserverEntityRenderer::new
        );

        ResidueClientCommands.register();
        ResidueClientConnectionEvents.register();
    }
}