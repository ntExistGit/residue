package com.upphorattexistera.residuemod.config;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.gui.controllers.LabelController;
import net.minecraft.text.Text;

public class DownloadStatusLabel {

    public enum State {
        IDLE,
        DOWNLOADING,
        DONE,
        CANCELLED,
        ERROR
    }

    private volatile State  state    = State.IDLE;
    private volatile int    percent  = 0;
    private volatile String errorMsg = "";

    private final Option<Text> option;

    public DownloadStatusLabel() {
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

    public void setProgress(int newPercent) {
        this.percent = Math.max(0, Math.min(100, newPercent));
        this.state   = State.DOWNLOADING;
        refresh();
    }

    public void setState(State newState) {
        if (newState != State.DOWNLOADING) this.percent = 0;
        this.state = newState;
        refresh();
    }

    public void setError(String message) {
        this.errorMsg = message != null ? message : "Unknown error";
        this.percent  = 0;
        this.state    = State.ERROR;
        refresh();
    }

    public State getState() {
        return state;
    }

    public void reset() {
        this.percent  = 0;
        this.errorMsg = "";
        this.state    = State.IDLE;
        refresh();
    }

    private void refresh() {
        option.requestSet(buildText());
    }

    private Text buildText() {
        return switch (state) {
            case IDLE        -> Text.literal(" ");
            case DOWNLOADING -> Text.literal(progressBar(percent));
            case DONE        -> Text.translatable("residue.label.download.done");
            case CANCELLED   -> Text.translatable("residue.label.download.cancelled");
            case ERROR       -> Text.translatable("residue.label.download.error", Text.literal(clip(errorMsg, 60)));
        };
    }

    private static String progressBar(int pct) {
        int filled = pct / 5;
        StringBuilder sb = new StringBuilder("§f[");
        for (int i = 0; i < 20; i++) {
            sb.append(i < filled ? "§a█" : "§8░");
        }
        sb.append("§f] §e").append(pct).append("%");
        return sb.toString();
    }

    private static String clip(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}