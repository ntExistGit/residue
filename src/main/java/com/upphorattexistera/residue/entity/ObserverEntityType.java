package com.upphorattexistera.residue.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ObserverEntityType {

    public static EntityType<ObserverEntity> OBSERVER;

    public static void register() {
        OBSERVER = Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of("residue", "observer"),
                FabricEntityTypeBuilder.<ObserverEntity>create(SpawnGroup.MISC, ObserverEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                        .build()
        );
    }
}