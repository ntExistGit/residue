package com.upphorattexistera.residue.config;

import java.util.Arrays;
import java.util.List;

public enum TTSTalker {
    // 0.6B CustomVoice
    QWEN_TALKER_0_6B_CUSTOM_Q4("0.6B CustomVoice Q4_K_M",
            "qwen-talker-0.6b-customvoice-Q4_K_M.gguf",
            "https://huggingface.co/Serveurperso/Qwen3-TTS-GGUF/resolve/main/qwen-talker-0.6b-customvoice-Q4_K_M.gguf",
            605, 4),
    QWEN_TALKER_0_6B_CUSTOM_Q8("0.6B CustomVoice Q8_0",
            "qwen-talker-0.6b-customvoice-Q8_0.gguf",
            "https://huggingface.co/Serveurperso/Qwen3-TTS-GGUF/resolve/main/qwen-talker-0.6b-customvoice-Q8_0.gguf",
            969, 4),
    QWEN_TALKER_0_6B_CUSTOM_BF16("0.6B CustomVoice BF16",
            "qwen-talker-0.6b-customvoice-BF16.gguf",
            "https://huggingface.co/Serveurperso/Qwen3-TTS-GGUF/resolve/main/qwen-talker-0.6b-customvoice-BF16.gguf",
            1870, 6),
    QWEN_TALKER_0_6B_CUSTOM_F32("0.6B CustomVoice F32",
            "qwen-talker-0.6b-customvoice-F32.gguf",
            "https://huggingface.co/Serveurperso/Qwen3-TTS-GGUF/resolve/main/qwen-talker-0.6b-customvoice-F32.gguf",
            3720, 8),

    // 1.7B CustomVoice (Default)
    QWEN_TALKER_1_7B_CUSTOM_Q4("1.7B CustomVoice Q4_K_M",
            "qwen-talker-1.7b-customvoice-Q4_K_M.gguf",
            "https://huggingface.co/Serveurperso/Qwen3-TTS-GGUF/resolve/main/qwen-talker-1.7b-customvoice-Q4_K_M.gguf",
            1200, 8),
    QWEN_TALKER_1_7B_CUSTOM_Q8("1.7B CustomVoice Q8_0",
            "qwen-talker-1.7b-customvoice-Q8_0.gguf",
            "https://huggingface.co/Serveurperso/Qwen3-TTS-GGUF/resolve/main/qwen-talker-1.7b-customvoice-Q8_0.gguf",
            2090, 12),
    QWEN_TALKER_1_7B_CUSTOM_BF16("1.7B CustomVoice BF16",
            "qwen-talker-1.7b-customvoice-BF16.gguf",
            "https://huggingface.co/Serveurperso/Qwen3-TTS-GGUF/resolve/main/qwen-talker-1.7b-customvoice-BF16.gguf",
            3930, 16),
    QWEN_TALKER_1_7B_CUSTOM_F32("1.7B CustomVoice F32",
            "qwen-talker-1.7b-customvoice-F32.gguf",
            "https://huggingface.co/Serveurperso/Qwen3-TTS-GGUF/resolve/main/qwen-talker-1.7b-customvoice-F32.gguf",
            7850, 24),

    CUSTOM("Custom Talker", "", "", 0, 0);

    public final String displayName;
    public final String fileName;
    public final String downloadUrl;
    public final int sizeMB;
    public final int minRAMGB;

    TTSTalker(String displayName, String fileName, String downloadUrl, int sizeMB, int minRAMGB) {
        this.displayName = displayName;
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.sizeMB = sizeMB;
        this.minRAMGB = minRAMGB;
    }

    public static List<String> getDisplayNames() {
        return Arrays.stream(values()).map(m -> m.displayName).toList();
    }

    public static TTSTalker fromDisplayName(String displayName) {
        for (TTSTalker model : values()) {
            if (model.displayName.equals(displayName)) return model;
        }
        return QWEN_TALKER_1_7B_CUSTOM_Q4;
    }
}