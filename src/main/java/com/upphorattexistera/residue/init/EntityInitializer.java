package com.upphorattexistera.residue.init;

import com.upphorattexistera.residue.entity.ObserverEntity;
import com.upphorattexistera.residue.entity.ObserverEntityType;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

public class EntityInitializer {
    public static void init() {
        ObserverEntityType.register();
        FabricDefaultAttributeRegistry.register(
                ObserverEntityType.OBSERVER,
                ObserverEntity.createAttributes()
        );
    }
}