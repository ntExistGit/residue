package com.upphorattexistera.residuemod.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ResidueConfigScreen {

    public static Screen create(Screen parent) {

        return YetAnotherConfigLib.createBuilder()
                .title(Text.translatable("residue.config.title"))

                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("residue.config.general"))

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("residue.config.enable_mod"))
                                .description(OptionDescription.of(
                                        Text.translatable("residue.config.enable_mod.desc")))
                                .binding(true,
                                        () -> ResidueConfig.INSTANCE.enableMod,
                                        value -> ResidueConfig.INSTANCE.enableMod = value)
                                .controller(BooleanControllerBuilder::create)
                                .build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("residue.config.debug"))
                                .description(OptionDescription.of(
                                        Text.translatable("residue.config.debug.desc")))
                                .binding(false,
                                        () -> ResidueConfig.INSTANCE.debugMode,
                                        value -> ResidueConfig.INSTANCE.debugMode = value)
                                .controller(BooleanControllerBuilder::create)
                                .build())

                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("residue.config.memory"))

                        .option(Option.<Integer>createBuilder()
                                .name(Text.translatable("residue.config.memory_seconds"))
                                .description(OptionDescription.of(
                                        Text.translatable("residue.config.memory_seconds.desc")))
                                .binding(60,
                                        () -> ResidueConfig.INSTANCE.memoryIncreaseSeconds,
                                        value -> ResidueConfig.INSTANCE.memoryIncreaseSeconds = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(5, 600)
                                        .step(5))
                                .build())

                        .option(Option.<Integer>createBuilder()
                                .name(Text.translatable("residue.config.max_memory"))
                                .description(OptionDescription.of(
                                        Text.translatable("residue.config.max_memory.desc")))
                                .binding(100,
                                        () -> ResidueConfig.INSTANCE.maxMemory,
                                        value -> ResidueConfig.INSTANCE.maxMemory = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(10, 1000)
                                        .step(10))
                                .build())

                        .build())

                .category(ConfigCategory.createBuilder()

                        .name(Text.translatable("residue.config.events"))

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("residue.config.dream"))
                                .description(OptionDescription.of(
                                        Text.translatable("residue.config.dream.desc")))
                                .binding(true,
                                        () -> ResidueConfig.INSTANCE.enableDreamEvent,
                                        value -> ResidueConfig.INSTANCE.enableDreamEvent = value)
                                .controller(opt -> BooleanControllerBuilder.create(opt)
                                        .trueFalseFormatter()
                                        .coloured(true))
                                .build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("residue.config.torch"))
                                .description(OptionDescription.of(
                                        Text.translatable("residue.config.torch.desc")))
                                .binding(true,
                                        () -> ResidueConfig.INSTANCE.enableDistantTorchEvent,
                                        value -> ResidueConfig.INSTANCE.enableDistantTorchEvent = value)
                                .controller(opt -> BooleanControllerBuilder.create(opt)
                                        .trueFalseFormatter()
                                        .coloured(true))
                                .build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("residue.config.clone"))
                                .description(OptionDescription.of(
                                        Text.translatable("residue.config.clone.desc")))
                                .binding(true,
                                        () -> ResidueConfig.INSTANCE.enableSelfCloneEvent,
                                        value -> ResidueConfig.INSTANCE.enableSelfCloneEvent = value)
                                .controller(opt -> BooleanControllerBuilder.create(opt)
                                        .trueFalseFormatter()
                                        .coloured(true))
                                .build())

                        .option(LabelOption.create(Text.translatable("residue.config.torch.label")))

                        .option(Option.<Integer>createBuilder()
                                .name(Text.translatable("residue.config.torch.min_distance"))
                                .description(OptionDescription.of(
                                        Text.translatable("residue.config.torch.min_distance.desc")))
                                .binding(80,
                                        () -> ResidueConfig.INSTANCE.torchMinDistance,
                                        value -> ResidueConfig.INSTANCE.torchMinDistance = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(20, 500).step(10))
                                .build())

                        .option(Option.<Integer>createBuilder()
                                .name(Text.translatable("residue.config.torch.max_distance"))
                                .description(OptionDescription.of(
                                        Text.translatable("residue.config.torch.max_distance.desc")))
                                .binding(200,
                                        () -> ResidueConfig.INSTANCE.torchMaxDistance,
                                        value -> ResidueConfig.INSTANCE.torchMaxDistance = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(20, 500).step(10))
                                .build())

                        .option(Option.<Integer>createBuilder()
                                .name(Text.translatable("residue.config.torch.despawn_seconds"))
                                .description(OptionDescription.of(
                                        Text.translatable("residue.config.torch.despawn_seconds.desc")))
                                .binding(30,
                                        () -> ResidueConfig.INSTANCE.torchDespawnSeconds,
                                        value -> ResidueConfig.INSTANCE.torchDespawnSeconds = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(5, 300).step(5))
                                .build())

                        .option(Option.<Double>createBuilder()
                                .name(Text.translatable("residue.config.torch.disappear_distance"))
                                .description(OptionDescription.of(
                                        Text.translatable("residue.config.torch.disappear_distance.desc")))
                                .binding(6.0,
                                        () -> ResidueConfig.INSTANCE.torchDisappearDistance,
                                        value -> ResidueConfig.INSTANCE.torchDisappearDistance = value)
                                .controller(opt -> DoubleSliderControllerBuilder.create(opt).range(2.0, 20.0).step(0.5))
                                .build())

                        .option(Option.<Integer>createBuilder()
                                .name(Text.translatable("residue.config.torch.max_active"))
                                .description(OptionDescription.of(Text.translatable("residue.config.torch.max_active.desc")))
                                .binding(3,
                                        () -> ResidueConfig.INSTANCE.torchMaxActive,
                                        value -> ResidueConfig.INSTANCE.torchMaxActive = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 20).step(1))
                                .build())

                        .option(Option.<Integer>createBuilder()
                                .name(Text.translatable("residue.config.torch.spawn_chance"))
                                .description(OptionDescription.of(Text.translatable("residue.config.torch.spawn_chance.desc")))
                                .binding(5,
                                        () -> ResidueConfig.INSTANCE.torchSpawnChance,
                                        value -> ResidueConfig.INSTANCE.torchSpawnChance = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 100).step(1))
                                .build())

                        .option(LabelOption.create(Text.translatable("residue.config.clone.label")))

                        .option(Option.<Integer>createBuilder()
                                .name(Text.translatable("residue.config.clone.min_distance"))
                                .description(OptionDescription.of(
                                        Text.translatable("residue.config.clone.min_distance.desc")))
                                .binding(40,
                                        () -> ResidueConfig.INSTANCE.selfCloneMinDistance,
                                        value -> ResidueConfig.INSTANCE.selfCloneMinDistance = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(10, 200).step(5))
                                .build())

                        .option(Option.<Integer>createBuilder()
                                .name(Text.translatable("residue.config.clone.max_distance"))
                                .description(OptionDescription.of(
                                        Text.translatable("residue.config.clone.max_distance.desc")))
                                .binding(80,
                                        () -> ResidueConfig.INSTANCE.selfCloneMaxDistance,
                                        value -> ResidueConfig.INSTANCE.selfCloneMaxDistance = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(10, 200).step(5))
                                .build())

                        .option(Option.<Integer>createBuilder()
                                .name(Text.translatable("residue.config.clone.cooldown"))
                                .description(OptionDescription.of(
                                        Text.translatable("residue.config.clone.cooldown.desc")))
                                .binding(300,
                                        () -> ResidueConfig.INSTANCE.selfCloneCooldownSeconds,
                                        value -> ResidueConfig.INSTANCE.selfCloneCooldownSeconds = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(30, 3600).step(30))
                                .build())

                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("residue.config.visual"))
                        .build())

                .save(ResidueConfigSerializer::save)
                .build()
                .generateScreen(parent);
    }
}