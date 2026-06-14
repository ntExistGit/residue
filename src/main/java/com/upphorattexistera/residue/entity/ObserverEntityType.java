package com.upphorattexistera.residue.entity;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ObserverEntityType {

    public static final RegistryKey<EntityType<?>> OBSERVER_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("residue", "observer"));

    public static EntityType<ObserverEntity> OBSERVER;

    public static void register() {
        OBSERVER = Registry.register(
                Registries.ENTITY_TYPE,
                OBSERVER_KEY,
                EntityType.Builder.create(ObserverEntity::new, SpawnGroup.MISC)
                        .dimensions(0.6f, 1.8f)
                        .build(OBSERVER_KEY)
        );
    }
}