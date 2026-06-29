package com.upphorattexistera.residue.config;

import com.upphorattexistera.residue.client.ai.LLMServerManager;
import com.upphorattexistera.residue.client.ai.TTSServerManager;
import com.upphorattexistera.residue.config.category.*;
import com.upphorattexistera.residue.observer.ObserverRaycastIgnoreResolver;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ResidueConfigScreen {

    public static Screen create(Screen parent) {
        // LLM статус-лейблы
        DownloadStatusLabel statusLabel = new DownloadStatusLabel();
        LLMServerManager.getInstance().setStatusLabel(statusLabel);

        LlmServerStatusLabel serverStatusLabel =
                new LlmServerStatusLabel(LLMServerManager.getInstance().isRunning());

        // TTS статус-лейблы
        DownloadStatusLabel ttsStatusLabel = new DownloadStatusLabel();
        TTSServerManager.getInstance().setStatusLabel(ttsStatusLabel);

        TtsServerStatusLabel ttsServerStatusLabel =
                new TtsServerStatusLabel();

        return YetAnotherConfigLib.createBuilder()
                .title(Text.translatable("residue.config.title"))
                .category(GeneralCategory.build())
                .category(MemoryCategory.build())
                .category(EventsCategory.build())
                .category(ObserverStageCategory.build())
                .category(VisualCategory.build())
                .category(AI_CPP_Category.build(statusLabel, serverStatusLabel, ttsStatusLabel, ttsServerStatusLabel))
                .save(() -> {
                    ResidueConfigSerializer.save();
                    ObserverRaycastIgnoreResolver.invalidate();
                })
                .build()
                .generateScreen(parent);
    }
}