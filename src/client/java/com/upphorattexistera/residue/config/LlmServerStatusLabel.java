package com.upphorattexistera.residue.config;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.gui.controllers.LabelController;
import net.minecraft.text.Text;

public class LlmServerStatusLabel {

    private volatile boolean running;
    private final Option<Text> option;

    public LlmServerStatusLabel(boolean initiallyRunning) {
        this.running = initiallyRunning;
        this.option = Option.<Text>createBuilder()
                .name(Text.empty())
                .description(OptionDescription.EMPTY)
                .binding(Text.empty(), this::buildText, val -> {})
                .customController(opt -> new LabelController(opt))
                .build();
    }

    public Option<Text> getOption() {
        return option;
    }

    public void setRunning(boolean running) {
        this.running = running;
        option.requestSet(buildText());
    }

    private Text buildText() {
        return running
                ? Text.translatable("residue.label.llm_server.running")
                : Text.translatable("residue.label.llm_server.stopped");
    }
}