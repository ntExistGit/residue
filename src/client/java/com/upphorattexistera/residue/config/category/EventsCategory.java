package com.upphorattexistera.residue.config.category;

import com.upphorattexistera.residue.config.ResidueConfig;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.text.Text;

public final class EventsCategory {

    private EventsCategory() {}

    public static ConfigCategory build() {
        return ConfigCategory.createBuilder()
                .name(Text.translatable("residue.config.events"))

                // --- Основные события ---

                .option(Option.<Boolean>createBuilder()
                        .name(Text.translatable("residue.config.dream"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.dream.desc")))
                        .binding(true,
                                () -> ResidueConfig.INSTANCE.enableDreamEvent,
                                value -> ResidueConfig.INSTANCE.enableDreamEvent = value)
                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                .trueFalseFormatter().coloured(true))
                        .build())

                .option(Option.<Boolean>createBuilder()
                        .name(Text.translatable("residue.config.torch"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.torch.desc")))
                        .binding(true,
                                () -> ResidueConfig.INSTANCE.enableDistantTorchEvent,
                                value -> ResidueConfig.INSTANCE.enableDistantTorchEvent = value)
                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                .trueFalseFormatter().coloured(true))
                        .build())

                .option(Option.<Boolean>createBuilder()
                        .name(Text.translatable("residue.config.clone"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.clone.desc")))
                        .binding(true,
                                () -> ResidueConfig.INSTANCE.enableSelfCloneEvent,
                                value -> ResidueConfig.INSTANCE.enableSelfCloneEvent = value)
                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                .trueFalseFormatter().coloured(true))
                        .build())

                // --- Fake LAN ---

                .option(LabelOption.create(Text.translatable("residue.config.label.fake_lan")))

                .option(Option.<Boolean>createBuilder()
                        .name(Text.translatable("residue.config.fake_lan"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.fake_lan.desc")))
                        .binding(true,
                                () -> ResidueConfig.INSTANCE.enableFakeLanEvent,
                                value -> ResidueConfig.INSTANCE.enableFakeLanEvent = value)
                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                .trueFalseFormatter().coloured(true))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.fake_lan.min_minutes"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.fake_lan.min_minutes.desc")))
                        .binding(5,
                                () -> ResidueConfig.INSTANCE.fakeLanMinMinutes,
                                value -> ResidueConfig.INSTANCE.fakeLanMinMinutes = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(1, 120).step(1))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.fake_lan.max_minutes"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.fake_lan.max_minutes.desc")))
                        .binding(20,
                                () -> ResidueConfig.INSTANCE.fakeLanMaxMinutes,
                                value -> ResidueConfig.INSTANCE.fakeLanMaxMinutes = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(1, 120).step(1))
                        .build())

                // --- Observer ---

                .option(LabelOption.create(Text.translatable("residue.config.observer.label")))

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.observer.max_simultaneous"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.observer.max_simultaneous.desc")))
                        .binding(2,
                                () -> ResidueConfig.INSTANCE.observerMaxSimultaneous,
                                value -> ResidueConfig.INSTANCE.observerMaxSimultaneous = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(1, 10).step(1))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.observer.connect_chance_lan"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.observer.connect_chance_lan.desc")))
                        .binding(50,
                                () -> ResidueConfig.INSTANCE.observerConnectChanceLan,
                                value -> ResidueConfig.INSTANCE.observerConnectChanceLan = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(1, 1000).step(1))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.observer.connect_chance_no_lan"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.observer.connect_chance_no_lan.desc")))
                        .binding(1,
                                () -> ResidueConfig.INSTANCE.observerConnectChanceNoLan,
                                value -> ResidueConfig.INSTANCE.observerConnectChanceNoLan = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(1, 100).step(1))
                        .build())

                .option(Option.<Double>createBuilder()
                        .name(Text.translatable("residue.config.observer.flap_chance"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.observer.flap_chance.desc")))
                        .binding(20.0,
                                () -> ResidueConfig.INSTANCE.observerFlapChance,
                                value -> ResidueConfig.INSTANCE.observerFlapChance = value)
                        .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                                .range(0.0, 100.0).step(1.0))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.observer.session_min"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.observer.session_min.desc")))
                        .binding(120,
                                () -> ResidueConfig.INSTANCE.observerSessionMinSeconds,
                                value -> ResidueConfig.INSTANCE.observerSessionMinSeconds = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(10, 600).step(10))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.observer.session_max"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.observer.session_max.desc")))
                        .binding(600,
                                () -> ResidueConfig.INSTANCE.observerSessionMaxSeconds,
                                value -> ResidueConfig.INSTANCE.observerSessionMaxSeconds = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(30, 3600).step(30))
                        .build())

                // --- Distant Torch ---

                .option(LabelOption.create(Text.translatable("residue.config.torch.label")))

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.torch.min_distance"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.torch.min_distance.desc")))
                        .binding(80,
                                () -> ResidueConfig.INSTANCE.torchMinDistance,
                                value -> ResidueConfig.INSTANCE.torchMinDistance = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(20, 500).step(10))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.torch.max_distance"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.torch.max_distance.desc")))
                        .binding(200,
                                () -> ResidueConfig.INSTANCE.torchMaxDistance,
                                value -> ResidueConfig.INSTANCE.torchMaxDistance = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(20, 500).step(10))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.torch.despawn_seconds"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.torch.despawn_seconds.desc")))
                        .binding(30,
                                () -> ResidueConfig.INSTANCE.torchDespawnSeconds,
                                value -> ResidueConfig.INSTANCE.torchDespawnSeconds = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(5, 300).step(5))
                        .build())

                .option(Option.<Double>createBuilder()
                        .name(Text.translatable("residue.config.torch.disappear_distance"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.torch.disappear_distance.desc")))
                        .binding(6.0,
                                () -> ResidueConfig.INSTANCE.torchDisappearDistance,
                                value -> ResidueConfig.INSTANCE.torchDisappearDistance = value)
                        .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                                .range(2.0, 20.0).step(0.5))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.torch.max_active"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.torch.max_active.desc")))
                        .binding(3,
                                () -> ResidueConfig.INSTANCE.torchMaxActive,
                                value -> ResidueConfig.INSTANCE.torchMaxActive = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(1, 20).step(1))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.torch.spawn_chance"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.torch.spawn_chance.desc")))
                        .binding(5,
                                () -> ResidueConfig.INSTANCE.torchSpawnChance,
                                value -> ResidueConfig.INSTANCE.torchSpawnChance = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(1, 100).step(1))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.torch.memory_min"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.torch.memory_min.desc")))
                        .binding(40,
                                () -> ResidueConfig.INSTANCE.torchMemoryMin,
                                value -> ResidueConfig.INSTANCE.torchMemoryMin = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(0, 100).step(5))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.torch.memory_max"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.torch.memory_max.desc")))
                        .binding(100,
                                () -> ResidueConfig.INSTANCE.torchMemoryMax,
                                value -> ResidueConfig.INSTANCE.torchMemoryMax = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(0, 100).step(5))
                        .build())

                // --- Self Clone ---

                .option(LabelOption.create(Text.translatable("residue.config.clone.label")))

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.clone.min_distance"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.clone.min_distance.desc")))
                        .binding(40,
                                () -> ResidueConfig.INSTANCE.selfCloneMinDistance,
                                value -> ResidueConfig.INSTANCE.selfCloneMinDistance = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(10, 200).step(5))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.clone.max_distance"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.clone.max_distance.desc")))
                        .binding(80,
                                () -> ResidueConfig.INSTANCE.selfCloneMaxDistance,
                                value -> ResidueConfig.INSTANCE.selfCloneMaxDistance = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(10, 200).step(5))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.clone.cooldown"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.clone.cooldown.desc")))
                        .binding(300,
                                () -> ResidueConfig.INSTANCE.selfCloneCooldownSeconds,
                                value -> ResidueConfig.INSTANCE.selfCloneCooldownSeconds = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(30, 3600).step(30))
                        .build())

                .build();
    }
}