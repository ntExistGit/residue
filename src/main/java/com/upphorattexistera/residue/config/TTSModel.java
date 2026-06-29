package com.upphorattexistera.residue.config;

import java.util.Arrays;
import java.util.List;

public enum TTSModel {
    QWEN_TTS_0_6B_BASE("Qwen3-TTS 0.6B Base",
            "Qwen3-TTS-12Hz-0.6B-Base-Q8_0.gguf",
            "https://huggingface.co/khimaros/Qwen3-TTS-12Hz-0.6B-Base-GGUF/resolve/main/Qwen3-TTS-12Hz-0.6B-Base-Q8_0.gguf",
            450, 4),

    QWEN_TTS_0_6B_CUSTOM("Qwen3-TTS 0.6B CustomVoice",
            "Qwen3-TTS-12Hz-0.6B-CustomVoice-Q8_0.gguf",
            "https://huggingface.co/khimaros/Qwen3-TTS-12Hz-0.6B-CustomVoice-GGUF/resolve/main/Qwen3-TTS-12Hz-0.6B-CustomVoice-Q8_0.gguf",
            450, 4),

    QWEN_TTS_1_7B_BASE("Qwen3-TTS 1.7B Base",
            "Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf",
            "https://huggingface.co/khimaros/Qwen3-TTS-12Hz-1.7B-Base-GGUF/resolve/main/Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf",
            1300, 8),

    QWEN_TTS_1_7B_CUSTOM("Qwen3-TTS 1.7B CustomVoice",
            "Qwen3-TTS-12Hz-1.7B-CustomVoice-Q8_0.gguf",
            "https://huggingface.co/khimaros/Qwen3-TTS-12Hz-1.7B-CustomVoice-GGUF/resolve/main/Qwen3-TTS-12Hz-1.7B-CustomVoice-Q8_0.gguf",
            1300, 8),

    QWEN_TTS_1_7B_DESIGN("Qwen3-TTS 1.7B VoiceDesign",
            "Qwen3-TTS-12Hz-1.7B-VoiceDesign-Q8_0.gguf",
            "https://huggingface.co/khimaros/Qwen3-TTS-12Hz-1.7B-VoiceDesign-GGUF/resolve/main/Qwen3-TTS-12Hz-1.7B-VoiceDesign-Q8_0.gguf",
            1300, 8),

    CUSTOM("Custom TTS model",
            "", "", 0, 0);

    public final String displayName;
    public final String fileName;
    public final String downloadUrl;
    public final int sizeMB;
    public final int minRAMGB;

    TTSModel(String displayName, String fileName, String downloadUrl,
             int sizeMB, int minRAMGB) {
        this.displayName = displayName;
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.sizeMB = sizeMB;
        this.minRAMGB = minRAMGB;
    }

    public static List<String> getDisplayNames() {
        return Arrays.stream(values())
                .map(m -> m.displayName)
                .toList();
    }

    public static TTSModel fromDisplayName(String displayName) {
        for (TTSModel model : values()) {
            if (model.displayName.equals(displayName)) {
                return model;
            }
        }
        return QWEN_TTS_0_6B_BASE;
    }

    public String toDisplayName() {
        return this.displayName;
    }
}