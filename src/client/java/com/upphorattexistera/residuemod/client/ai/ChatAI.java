package com.upphorattexistera.residuemod.client.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ChatAI {

    private static final String API_URL = "http://localhost:8080/v1/chat/completions";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Отправляет сообщение игрока на локальный llama-server и получает ответ.
     */
    public static Text ask(String userMessage) {
        if (!LLMServerManager.getInstance().isRunning()) {
            return Text.literal("§c[Residue] AI Server is not running. Start it in settings or wait for auto-start.");
        }

        try {
            JsonObject jsonBody = new JsonObject();
            JsonArray messages = new JsonArray();

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userMessage);
            messages.add(userMsg);

            jsonBody.add("messages", messages);
            jsonBody.addProperty("temperature", 0.7);
            jsonBody.addProperty("max_tokens", 256);
            jsonBody.addProperty("stream", false);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                String content = jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();

                return Text.literal("§5[AI] §r" + content.trim());
            } else {
                return Text.literal("§c[Residue] AI Error: HTTP " + response.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Text.literal("§c[Residue] Failed to contact AI: " + e.getMessage());
        }
    }
}