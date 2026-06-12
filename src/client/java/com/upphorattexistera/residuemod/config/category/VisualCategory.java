package com.upphorattexistera.residuemod.config.category;

import dev.isxander.yacl3.api.ConfigCategory;
import net.minecraft.text.Text;

public final class VisualCategory {

    private VisualCategory() {}

    public static ConfigCategory build() {
        return ConfigCategory.createBuilder()
                .name(Text.translatable("residue.config.visual"))
                // TODO: добавить опции
                .build();
    }
}