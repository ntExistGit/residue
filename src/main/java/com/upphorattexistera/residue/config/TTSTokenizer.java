package com.upphorattexistera.residue.config;

import java.util.Arrays;
import java.util.List;

public enum TTSTokenizer {
    Q4_K_M("Tokenizer Q4_K_M",
            "qwen-tokenizer-12hz-Q4_K_M.gguf",
            "https://huggingface.co/Serveurperso/Qwen3-TTS-GGUF/resolve/main/qwen-tokenizer-12hz-Q4_K_M.gguf",
            255),
    Q8_0("Tokenizer Q8_0",
            "qwen-tokenizer-12hz-Q8_0.gguf",
            "https://huggingface.co/Serveurperso/Qwen3-TTS-GGUF/resolve/main/qwen-tokenizer-12hz-Q8_0.gguf",
            291),
    BF16("Tokenizer BF16",
            "qwen-tokenizer-12hz-BF16.gguf",
            "https://huggingface.co/Serveurperso/Qwen3-TTS-GGUF/resolve/main/qwen-tokenizer-12hz-BF16.gguf",
            359),
    F32("Tokenizer F32",
            "qwen-tokenizer-12hz-F32.gguf",
            "https://huggingface.co/Serveurperso/Qwen3-TTS-GGUF/resolve/main/qwen-tokenizer-12hz-F32.gguf",
            647),
    CUSTOM("Custom Tokenizer", "", "", 0);

    public final String displayName;
    public final String fileName;
    public final String downloadUrl;
    public final int sizeMB;

    TTSTokenizer(String displayName, String fileName, String downloadUrl, int sizeMB) {
        this.displayName = displayName;
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.sizeMB = sizeMB;
    }

    public static List<String> getDisplayNames() {
        return Arrays.stream(values()).map(m -> m.displayName).toList();
    }

    public static TTSTokenizer fromDisplayName(String displayName) {
        for (TTSTokenizer model : values()) {
            if (model.displayName.equals(displayName)) return model;
        }
        return Q4_K_M;
    }
}