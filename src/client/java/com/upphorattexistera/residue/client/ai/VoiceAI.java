package com.upphorattexistera.residue.client.ai;

import com.upphorattexistera.residue.config.Language;
import com.upphorattexistera.residue.config.ResidueConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class VoiceAI {
    private static final String API_URL = "http://localhost:8081/v1/audio/speech";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static byte[] synthesize(String text, String speaker) {
        if (!TTSServerManager.getInstance().isRunning()) return null;

        try {
            String langCode = ResidueConfig.INSTANCE.language.ttsCode;

            StringBuilder jsonBody = new StringBuilder();
            jsonBody.append("{");
            jsonBody.append("\"input\":\"").append(escapeJson(text)).append("\",");
            jsonBody.append("\"voice\":\"").append(speaker != null ? speaker : "alloy").append("\","); // API qwen-tts использует voice
            jsonBody.append("\"language\":\"").append(langCode).append("\",");
            jsonBody.append("\"response_format\":\"wav\"");
            jsonBody.append("}");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<byte[]> response = client.send(
                    request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.err.println("[Residue] TTS error: HTTP " + response.statusCode());
            }

        } catch (Exception e) {
            System.err.println("[Residue] VoiceAI error: " + e.getMessage());
        }
        return null;
    }

    public static byte[] synthesize(String text, String speaker, Language language) {
        if (!TTSServerManager.getInstance().isRunning()) return null;

        try {
            String langCode = language.ttsCode;

            StringBuilder jsonBody = new StringBuilder();
            jsonBody.append("{");
            jsonBody.append("\"input\":\"").append(escapeJson(text)).append("\",");
            jsonBody.append("\"voice\":\"").append(speaker != null ? speaker : "alloy").append("\",");
            jsonBody.append("\"language\":\"").append(langCode).append("\",");
            jsonBody.append("\"response_format\":\"wav\"");
            jsonBody.append("}");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<byte[]> response = client.send(
                    request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.err.println("[Residue] TTS error: HTTP " + response.statusCode());
            }

        } catch (Exception e) {
            System.err.println("[Residue] VoiceAI error: " + e.getMessage());
        }
        return null;
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}