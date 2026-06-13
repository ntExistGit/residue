package com.upphorattexistera.residue.config;

public enum LLMBackend {
    AUTO("Auto", "auto"),
    CPU("CPU", "cpu"),
    VULKAN("Vulkan", "vulkan"),
    CUDA12("Nvidia (CUDA 12)", "cuda12"),
    CUDA13("Nvidia (CUDA 13)", "cuda13"),
    HIP("AMD (HIP/ROCm)", "hip");

    public final String displayName;
    public final String id;

    LLMBackend(String displayName, String id) {
        this.displayName = displayName;
        this.id = id;
    }

    public String getDownloadUrl() {
        String baseUrl = "https://github.com/ggml-org/llama.cpp/releases/download/b9592/";
        return switch (this) {
            case CPU -> baseUrl + "llama-b9592-bin-win-cpu-x64.zip";
            case VULKAN -> baseUrl + "llama-b9592-bin-win-vulkan-x64.zip";
            case CUDA12 -> baseUrl + "llama-b9592-bin-win-cuda-12.4-x64.zip";
            case CUDA13 -> baseUrl + "llama-b9592-bin-win-cuda-13.3-x64.zip";
            case HIP -> baseUrl + "llama-b9592-bin-win-hip-radeon-x64.zip";
            default -> null;
        };
    }
}
