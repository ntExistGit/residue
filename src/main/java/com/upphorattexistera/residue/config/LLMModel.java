package com.upphorattexistera.residue.config;

import java.util.Arrays;
import java.util.List;

public enum LLMModel {
    QWEN_3B("Qwen 2.5 3B",
            "Qwen2.5-3B-Instruct-Q4_K_M.gguf",
            "https://huggingface.co/bartowski/Qwen2.5-3B-Instruct-GGUF/resolve/main/Qwen2.5-3B-Instruct-Q4_K_M.gguf",
            1900, 4, 2048),

    QWEN_7B("Qwen 2.5 7B",
            "Qwen2.5-7B-Instruct-Q4_K_M.gguf",
            "https://huggingface.co/bartowski/Qwen2.5-7B-Instruct-GGUF/resolve/main/Qwen2.5-7B-Instruct-Q4_K_M.gguf",
            4500, 8, 2048),

    QWEN_14B("Qwen 2.5 14B",
            "Qwen2.5-14B-Instruct-Q4_K_M.gguf",
            "https://huggingface.co/bartowski/Qwen2.5-14B-Instruct-GGUF/resolve/main/Qwen2.5-14B-Instruct-Q4_K_M.gguf",
            8900, 12, 2048),

    GEMMA_9B("Gemma 2 9B",
            "gemma-2-9b-it-Q4_K_M.gguf",
            "https://huggingface.co/bartowski/gemma-2-9b-it-GGUF/resolve/main/gemma-2-9b-it-Q4_K_M.gguf",
            5400, 8, 2048),

    MISTRAL_7B("Mistral 7B",
            "Mistral-7B-Instruct-v0.3-Q4_K_M.gguf",
            "https://huggingface.co/bartowski/Mistral-7B-Instruct-v0.3-GGUF/resolve/main/Mistral-7B-Instruct-v0.3-Q4_K_M.gguf",
            4400, 8, 2048),

    CUSTOM("Custom model",
            "", "", 0, 0, 2048);

    public final String displayName;
    public final String fileName;
    public final String downloadUrl;
    public final int sizeMB;
    public final int minRAMGB;
    private final int ctxSize;

    LLMModel(String displayName, String fileName, String downloadUrl,
             int sizeMB, int minRAMGB, int ctxSize) {
        this.displayName = displayName;
        this.fileName    = fileName;
        this.downloadUrl = downloadUrl;
        this.sizeMB      = sizeMB;
        this.minRAMGB    = minRAMGB;
        this.ctxSize     = ctxSize;
    }

    /** Размер контекста для передачи в --ctx-size */
    public int contextSize() {
        return ctxSize;
    }

    public static List<String> getDisplayNames() {
        return Arrays.stream(values())
                .map(m -> m.displayName)
                .toList();
    }

    public static LLMModel fromDisplayName(String displayName) {
        for (LLMModel model : values()) {
            if (model.displayName.equals(displayName)) {
                return model;
            }
        }
        return QWEN_3B;
    }

    public String toDisplayName() {
        return this.displayName;
    }
}