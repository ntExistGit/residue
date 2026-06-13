package com.upphorattexistera.residue.config.category;

import com.upphorattexistera.residue.config.ResidueConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import net.minecraft.text.Text;

public final class MemoryCategory {

    private MemoryCategory() {}

    public static ConfigCategory build() {
        return ConfigCategory.createBuilder()
                .name(Text.translatable("residue.config.memory"))

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.memory_seconds"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.memory_seconds.desc")))
                        .binding(60,
                                () -> ResidueConfig.INSTANCE.memoryIncreaseSeconds,
                                value -> ResidueConfig.INSTANCE.memoryIncreaseSeconds = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(5, 600).step(5))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.max_memory"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.max_memory.desc")))
                        .binding(100,
                                () -> ResidueConfig.INSTANCE.maxMemory,
                                value -> ResidueConfig.INSTANCE.maxMemory = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(10, 1000).step(10))
                        .build())

                .build();
    }
}