package com.upphorattexistera.residue.config.category;

import com.upphorattexistera.residue.config.RaycastIgnore;
import com.upphorattexistera.residue.config.ResidueConfig;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class ObserverStageCategory {

    private ObserverStageCategory() {}

    public static ConfigCategory build() {
        ConfigCategory.Builder builder = ConfigCategory.createBuilder()
                .name(Text.translatable("residue.config.observer_stage"));

        // Дефолты соответствуют значениям полей в ResidueConfig.java
        addStageGroup(builder, 0, 30, 70, 0, 0,
                () -> ResidueConfig.INSTANCE.observerStage0MinSpawnDistance,
                v -> ResidueConfig.INSTANCE.observerStage0MinSpawnDistance = v,
                () -> ResidueConfig.INSTANCE.observerStage0MaxSpawnDistance,
                v -> ResidueConfig.INSTANCE.observerStage0MaxSpawnDistance = v,
                () -> ResidueConfig.INSTANCE.observerStage0WatchDistance,
                v -> ResidueConfig.INSTANCE.observerStage0WatchDistance = v,
                () -> ResidueConfig.INSTANCE.observerStage0CriticalDistance,
                v -> ResidueConfig.INSTANCE.observerStage0CriticalDistance = v);

        addStageGroup(builder, 1, 30, 70, 12, 4,
                () -> ResidueConfig.INSTANCE.observerStage1MinSpawnDistance,
                v -> ResidueConfig.INSTANCE.observerStage1MinSpawnDistance = v,
                () -> ResidueConfig.INSTANCE.observerStage1MaxSpawnDistance,
                v -> ResidueConfig.INSTANCE.observerStage1MaxSpawnDistance = v,
                () -> ResidueConfig.INSTANCE.observerStage1WatchDistance,
                v -> ResidueConfig.INSTANCE.observerStage1WatchDistance = v,
                () -> ResidueConfig.INSTANCE.observerStage1CriticalDistance,
                v -> ResidueConfig.INSTANCE.observerStage1CriticalDistance = v);

        addStageGroup(builder, 2, 25, 60, 14, 5,
                () -> ResidueConfig.INSTANCE.observerStage2MinSpawnDistance,
                v -> ResidueConfig.INSTANCE.observerStage2MinSpawnDistance = v,
                () -> ResidueConfig.INSTANCE.observerStage2MaxSpawnDistance,
                v -> ResidueConfig.INSTANCE.observerStage2MaxSpawnDistance = v,
                () -> ResidueConfig.INSTANCE.observerStage2WatchDistance,
                v -> ResidueConfig.INSTANCE.observerStage2WatchDistance = v,
                () -> ResidueConfig.INSTANCE.observerStage2CriticalDistance,
                v -> ResidueConfig.INSTANCE.observerStage2CriticalDistance = v);

        addStageGroup(builder, 3, 20, 50, 16, 6,
                () -> ResidueConfig.INSTANCE.observerStage3MinSpawnDistance,
                v -> ResidueConfig.INSTANCE.observerStage3MinSpawnDistance = v,
                () -> ResidueConfig.INSTANCE.observerStage3MaxSpawnDistance,
                v -> ResidueConfig.INSTANCE.observerStage3MaxSpawnDistance = v,
                () -> ResidueConfig.INSTANCE.observerStage3WatchDistance,
                v -> ResidueConfig.INSTANCE.observerStage3WatchDistance = v,
                () -> ResidueConfig.INSTANCE.observerStage3CriticalDistance,
                v -> ResidueConfig.INSTANCE.observerStage3CriticalDistance = v);

        addStageGroup(builder, 4, 15, 40, 20, 8,
                () -> ResidueConfig.INSTANCE.observerStage4MinSpawnDistance,
                v -> ResidueConfig.INSTANCE.observerStage4MinSpawnDistance = v,
                () -> ResidueConfig.INSTANCE.observerStage4MaxSpawnDistance,
                v -> ResidueConfig.INSTANCE.observerStage4MaxSpawnDistance = v,
                () -> ResidueConfig.INSTANCE.observerStage4WatchDistance,
                v -> ResidueConfig.INSTANCE.observerStage4WatchDistance = v,
                () -> ResidueConfig.INSTANCE.observerStage4CriticalDistance,
                v -> ResidueConfig.INSTANCE.observerStage4CriticalDistance = v);

        builder.option(LabelOption.create(Text.translatable("residue.config.observer_raycast_ignore.label")));

        builder.option(Option.<Float>createBuilder()
                .name(Text.translatable(""))
                .description(OptionDescription.of(
                        Text.translatable("")))
                .binding(120.0f,
                        () -> ResidueConfig.INSTANCE.observerRaycastAngleDegrees,
                        value -> ResidueConfig.INSTANCE.observerRaycastAngleDegrees = value)
                .controller(opt -> FloatSliderControllerBuilder.create(opt)
                        .range(60.0f, 180.0f).step(1.0f))
                .build());

        builder.group(ListOption.<String>createBuilder()
                .name(Text.translatable("residue.config.observer_raycast_ignore"))
                .description(OptionDescription.of(
                        Text.translatable("residue.config.observer_raycast_ignore.desc")))
                .binding(
                        RaycastIgnore.getAllRawIds(),
                        () -> ResidueConfig.INSTANCE.observerRaycastIgnoreBlocks,
                        value -> ResidueConfig.INSTANCE.observerRaycastIgnoreBlocks = value)
                .controller(StringControllerBuilder::create)
                .initial("")
                .build());

        return builder.build();
    }

    @FunctionalInterface
    private interface Getter { int get(); }

    @FunctionalInterface
    private interface Setter { void set(int value); }

    private static void addStageGroup(ConfigCategory.Builder builder, int stage,
                                      int defaultMin, int defaultMax,
                                      int defaultWatch, int defaultCritical,
                                      Getter minGet, Setter minSet,
                                      Getter maxGet, Setter maxSet,
                                      Getter watchGet, Setter watchSet,
                                      Getter critGet, Setter critSet) {

        String prefix = "residue.config.observer_stage." + stage;

        builder.option(LabelOption.create(Text.translatable(prefix + ".label")));

        builder.option(Option.<Integer>createBuilder()
                .name(Text.translatable(prefix + ".min_spawn_distance"))
                .description(OptionDescription.of(
                        Text.translatable(prefix + ".min_spawn_distance.desc")))
                .binding(defaultMin, minGet::get, minSet::set)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                        .range(5, 300).step(5))
                .build());

        builder.option(Option.<Integer>createBuilder()
                .name(Text.translatable(prefix + ".max_spawn_distance"))
                .description(OptionDescription.of(
                        Text.translatable(prefix + ".max_spawn_distance.desc")))
                .binding(defaultMax, maxGet::get, maxSet::set)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                        .range(5, 300).step(5))
                .build());

        builder.option(Option.<Integer>createBuilder()
                .name(Text.translatable(prefix + ".watch_distance"))
                .description(OptionDescription.of(
                        Text.translatable(prefix + ".watch_distance.desc")))
                .binding(defaultWatch, watchGet::get, watchSet::set)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                        .range(0, 100).step(1))
                .build());

        builder.option(Option.<Integer>createBuilder()
                .name(Text.translatable(prefix + ".critical_distance"))
                .description(OptionDescription.of(
                        Text.translatable(prefix + ".critical_distance.desc")))
                .binding(defaultCritical, critGet::get, critSet::set)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                        .range(0, 50).step(1))
                .build());
    }
}