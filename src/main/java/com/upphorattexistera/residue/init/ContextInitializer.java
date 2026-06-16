package com.upphorattexistera.residue.init;

import com.upphorattexistera.residue.observer.context.ObserverContextRegistry;
import com.upphorattexistera.residue.observer.context.provider.*;

public class ContextInitializer {
    public static void init() {
        ObserverContextRegistry.register(new PositionContextProvider());
        ObserverContextRegistry.register(new WorldContextProvider());
        ObserverContextRegistry.register(new HealthContextProvider());
        ObserverContextRegistry.register(new InventoryContextProvider());
        ObserverContextRegistry.register(new PlayerStateContextProvider());
    }
}