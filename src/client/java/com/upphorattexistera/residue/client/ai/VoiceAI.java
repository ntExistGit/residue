package com.upphorattexistera.residue.client.ai;

import com.upphorattexistera.residue.config.Language;
import com.upphorattexistera.residue.config.ResidueConfig;
import com.upphorattexistera.residue.config.TTSSpeakers;

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

    /**
     * Синтезирует речь через qwentts.cpp tts-server.
     * @param speaker конкретный голос (ObserverAssignment.ttsSpeaker), не null/пусто
     * @return WAV (s16le, 24kHz, mono) или null при ошибке/выключенном сервере
     */
    public static byte[] synthesize(String text, String speaker) {
        if (!TTSServerManager.getInstance().isRunning()) return null;
        if (text == null || text.isBlank()) return null;

        try {
            StringBuilder jsonBody = new StringBuilder();
            jsonBody.append("{");
            jsonBody.append("\"input\":\"").append(escapeJson(text)).append("\",");
            jsonBody.append("\"voice\":\"").append(escapeJson(
                    speaker != null && !speaker.isBlank() ? speaker : TTSSpeakers.ALL.get(0)
            )).append("\",");
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
                System.err.println("[Residue] TTS error: HTTP " + response.statusCode()
                        + " body=" + new String(response.body()));
            }

        } catch (Exception e) {
            System.err.println("[Residue] VoiceAI error: " + e.getMessage());
        }
        return null;
    }

    // language-параметр упразднён: сервер его не принимает (см. tts-server.h —
    // парсит только input/voice/instructions/response_format/speed).
    // Язык реплики уже задаётся текстом самого ответа LLM.

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}