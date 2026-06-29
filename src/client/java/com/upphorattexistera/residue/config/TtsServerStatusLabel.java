package com.upphorattexistera.residue.config;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.gui.controllers.LabelController;
import net.minecraft.text.Text;

public class TtsServerStatusLabel {
    private volatile boolean running = false;
    private final Option<Text> option;

    public TtsServerStatusLabel() {
        this.option = Option.<Text>createBuilder()
                .name(Text.empty())
                .description(OptionDescription.EMPTY)
                .binding(
                        Text.empty(),
                        this::buildText,
                        val -> {}
                )
                .customController(opt -> new LabelController(opt))
                .build();
    }

    public Option<Text> getOption() {
        return option;
    }

    public void setRunning(boolean isRunning) {
        this.running = isRunning;
        refresh();
    }

    public boolean isRunning() {
        return running;
    }

    private void refresh() {
        option.requestSet(buildText());
    }

    private Text buildText() {
        return running
                ? Text.translatable("residue.label.tts_server.running")
                : Text.translatable("residue.label.tts_server.stopped");
    }
}