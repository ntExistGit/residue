package com.upphorattexistera.residue.client.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.upphorattexistera.residue.network.ObserverHistoryUpdatePacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatAI {

    private static final String API_URL = "http://localhost:8080/v1/chat/completions";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // История диалогов: имя обсервера → история сообщений
    private static final Map<String, JsonArray> conversationHistory = new ConcurrentHashMap<>();

    /**
     * Отправляет сообщение игрока обсерверу и получает ответ от его имени.
     */
    public static String askAsObserver(String observerName, String playerMessage,
                                       String systemPrompt, double temperature,
                                       int maxTokens, String historyJson) {
        if (!LLMServerManager.getInstance().isRunning()) return null;

        try {
            JsonArray history = JsonParser.parseString(historyJson).getAsJsonArray();

            JsonObject jsonBody = new JsonObject();
            JsonArray messages = new JsonArray();

            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", systemPrompt);
            messages.add(systemMsg);

            messages.addAll(history);

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", playerMessage);
            messages.add(userMsg);

            jsonBody.add("messages", messages);
            jsonBody.addProperty("temperature", temperature);
            jsonBody.addProperty("max_tokens", maxTokens);
            jsonBody.addProperty("stream", false);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String content = JsonParser.parseString(response.body())
                        .getAsJsonObject()
                        .getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString().trim();

                // Отправляем историю на сервер для сохранения
                ClientPlayNetworking.send(
                        new ObserverHistoryUpdatePacket.Payload(
                                observerName, playerMessage, content));

                return content;
            }

        } catch (Exception e) {
            System.err.println("[Residue] ChatAI error: " + e.getMessage());
        }
        return null;
    }

    public static void clearHistory(String observerName) {
        conversationHistory.remove(observerName);
    }

    public static void clearAllHistory() {
        conversationHistory.clear();
    }
}