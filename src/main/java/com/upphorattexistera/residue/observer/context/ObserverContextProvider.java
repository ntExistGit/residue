package com.upphorattexistera.residue.observer.context;

import net.minecraft.server.network.ServerPlayerEntity;
import java.util.Map;

public interface ObserverContextProvider {
    String getId();

    Map<String, String> provide(ServerPlayerEntity player);
}