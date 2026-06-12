package com.upphorattexistera.residuemod.config.category;

import com.upphorattexistera.residuemod.config.ResidueConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import net.minecraft.text.Text;

public final class GeneralCategory {

    private GeneralCategory() {}

    public static ConfigCategory build() {
        return ConfigCategory.createBuilder()
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

                .build();
    }
}