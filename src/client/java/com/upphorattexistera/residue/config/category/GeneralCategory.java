package com.upphorattexistera.residue.config.category;

import com.upphorattexistera.residue.config.ResidueConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
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

                .option(Option.<Boolean>createBuilder()
                        .name(Text.translatable(""))
                        .description(OptionDescription.of(
                                Text.translatable("")))
                        .binding(true,
                                () -> ResidueConfig.INSTANCE.enableTwitchIntegration,
                                value -> ResidueConfig.INSTANCE.enableTwitchIntegration = value)
                        .controller(BooleanControllerBuilder::create)
                        .build())

                .option(Option.<String>createBuilder()
                        .name(Text.translatable(""))
                        .description(OptionDescription.of(
                                Text.translatable("")))
                        .binding("",
                                () -> ResidueConfig.INSTANCE.twitchChannel,
                                value -> ResidueConfig.INSTANCE.twitchChannel = value)
                        .controller(StringControllerBuilder::create)
                        .build())

                .option(Option.<Boolean>createBuilder()
                        .name(Text.translatable(""))
                        .description(OptionDescription.of(
                                Text.translatable("")))
                        .binding(true,
                                () -> ResidueConfig.INSTANCE.enableVoiceChatIntegration,
                                value -> ResidueConfig.INSTANCE.enableVoiceChatIntegration = value)
                        .controller(BooleanControllerBuilder::create)
                        .build())

                .option(Option.<Double>createBuilder()
                        .name(Text.translatable(""))
                        .description(OptionDescription.of(
                                Text.translatable("")))
                        .binding(1.0,
                                () -> ResidueConfig.INSTANCE.voiceAttentionMultiplier,
                                value -> ResidueConfig.INSTANCE.voiceAttentionMultiplier = value)
                        .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                                .range(0.0, 10.0).step(0.1))
                        .build())

                .build();
    }
}