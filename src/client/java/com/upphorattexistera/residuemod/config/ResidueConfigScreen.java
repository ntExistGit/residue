package com.upphorattexistera.residuemod.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
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
                                .binding(
                                        true,
                                        () -> ResidueConfig.INSTANCE.enableMod,
                                        value -> ResidueConfig.INSTANCE.enableMod = value
                                )
                                .controller(opt -> BooleanControllerBuilder.create(opt))
                                .build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("residue.config.debug"))
                                .binding(
                                        false,
                                        () -> ResidueConfig.INSTANCE.debugMode,
                                        value -> ResidueConfig.INSTANCE.debugMode = value
                                )
                                .controller(opt -> BooleanControllerBuilder.create(opt))
                                .build())

                        .build())

                .category(ConfigCategory.createBuilder()

                        .name(Text.translatable("residue.config.memory"))

                        .option(Option.<Integer>createBuilder()
                                .name(Text.translatable("residue.config.memory_seconds"))
                                .binding(
                                        60,
                                        () -> ResidueConfig.INSTANCE.memoryIncreaseSeconds,
                                        value -> ResidueConfig.INSTANCE.memoryIncreaseSeconds = value
                                )
                                .controller(opt ->
                                        IntegerSliderControllerBuilder.create(opt)
                                                .range(5, 600)
                                                .step(5))
                                .build())

                        .option(Option.<Integer>createBuilder()
                                .name(Text.translatable("residue.config.max_memory"))
                                .binding(
                                        100,
                                        () -> ResidueConfig.INSTANCE.maxMemory,
                                        value -> ResidueConfig.INSTANCE.maxMemory = value
                                )
                                .controller(opt ->
                                        IntegerSliderControllerBuilder.create(opt)
                                                .range(10, 1000)
                                                .step(10))
                                .build())

                        .build())

                .category(ConfigCategory.createBuilder()

                        .name(Text.translatable("residue.config.events"))

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("residue.config.dream"))
                                .binding(
                                        true,
                                        () -> ResidueConfig.INSTANCE.enableDreamEvent,
                                        value -> ResidueConfig.INSTANCE.enableDreamEvent = value
                                )
                                .controller(opt -> BooleanControllerBuilder.create(opt))
                                .build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("residue.config.torch"))
                                .binding(
                                        true,
                                        () -> ResidueConfig.INSTANCE.enableDistantTorchEvent,
                                        value -> ResidueConfig.INSTANCE.enableDistantTorchEvent = value
                                )
                                .controller(opt -> BooleanControllerBuilder.create(opt))
                                .build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("residue.config.clone"))
                                .binding(
                                        true,
                                        () -> ResidueConfig.INSTANCE.enableSelfCloneEvent,
                                        value -> ResidueConfig.INSTANCE.enableSelfCloneEvent = value
                                )
                                .controller(opt -> BooleanControllerBuilder.create(opt))
                                .build())

                        .build())

                .save(ResidueConfigSerializer::save)

                .build()

                .generateScreen(parent);
    }
}
