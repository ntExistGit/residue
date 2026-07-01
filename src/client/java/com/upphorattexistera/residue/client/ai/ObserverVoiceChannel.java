package com.upphorattexistera.residue.client.ai;

public enum ObserverVoiceChannel {
    /** Звук идёт от позиции ObserverEntity в мире, затухает с расстоянием. */
    POSITIONAL,
    /** Звук "в голову" игроку напрямую, без позиции — для ивентовых шёпотов. */
    WHISPER
}