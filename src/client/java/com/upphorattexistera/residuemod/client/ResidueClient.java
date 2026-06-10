package com.upphorattexistera.residuemod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class ResidueClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("sway")) {
            SwayCompatLoader.register();
        }
    }
}