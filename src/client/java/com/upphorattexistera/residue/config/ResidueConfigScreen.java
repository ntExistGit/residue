package com.upphorattexistera.residue.config;

import com.upphorattexistera.residue.client.ai.LLMServerManager;
import com.upphorattexistera.residue.config.category.*;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ResidueConfigScreen {

    public static Screen create(Screen parent) {
        DownloadStatusLabel statusLabel = new DownloadStatusLabel();
        LLMServerManager.getInstance().setStatusLabel(statusLabel);

        return YetAnotherConfigLib.createBuilder()
                .title(Text.translatable("residue.config.title"))
                .category(GeneralCategory.build())
                .category(MemoryCategory.build())
                .category(EventsCategory.build())
                .category(VisualCategory.build())
                .category(LlamaCategory.build(statusLabel))
                .save(ResidueConfigSerializer::save)
                .build()
                .generateScreen(parent);
    }
}