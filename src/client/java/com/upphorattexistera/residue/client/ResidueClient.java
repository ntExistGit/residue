package com.upphorattexistera.residue.client;

import com.upphorattexistera.residue.client.ai.ResidueClientCommands;
import com.upphorattexistera.residue.client.ai.ResidueClientConnectionEvents;
import com.upphorattexistera.residue.client.render.ObserverEntityRenderer;
import com.upphorattexistera.residue.entity.ObserverEntityType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.entity.EntityRendererFactories;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ResidueClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        registerBuiltinResourcePack();

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

    private void registerBuiltinResourcePack() {
        FabricLoader.getInstance().getModContainer("residue").ifPresent(container ->
                ResourceLoader.registerBuiltinPack(
                        Identifier.of("residue", "residue"),
                        container,
                        Text.literal("Residue ResourcePack"),
                        PackActivationType.DEFAULT_ENABLED
                )
        );
    }
}