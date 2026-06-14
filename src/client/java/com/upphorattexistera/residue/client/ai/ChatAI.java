package com.upphorattexistera.residue.client.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
    public static String askAsObserver(String observerName, String playerMessage) {
        if (!LLMServerManager.getInstance().isRunning()) {
            return null;
        }

        try {
            JsonArray history = conversationHistory.computeIfAbsent(
                    observerName, k -> new JsonArray());

            JsonObject jsonBody = new JsonObject();
            JsonArray messages = new JsonArray();

            // Системный промпт — личность обсервера
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", buildSystemPrompt(observerName));
            messages.add(systemMsg);

            // Добавляем историю диалога
            messages.addAll(history);

            // Новое сообщение игрока
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", playerMessage);
            messages.add(userMsg);

            jsonBody.add("messages", messages);
            jsonBody.addProperty("temperature", 0.85);
            jsonBody.addProperty("max_tokens", 128);
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
                JsonObject jsonResponse = JsonParser.parseString(response.body())
                        .getAsJsonObject();
                String content = jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString().trim();

                // Сохраняем в историю
                JsonObject savedUser = new JsonObject();
                savedUser.addProperty("role", "user");
                savedUser.addProperty("content", playerMessage);
                history.add(savedUser);

                JsonObject savedAssistant = new JsonObject();
                savedAssistant.addProperty("role", "assistant");
                savedAssistant.addProperty("content", content);
                history.add(savedAssistant);

                // Ограничиваем историю последними 20 сообщениями
                while (history.size() > 20) {
                    history.remove(0);
                }

                return content;
            }

        } catch (Exception e) {
            System.err.println("[Residue] ChatAI error: " + e.getMessage());
        }

        return null;
    }

    private static String buildSystemPrompt(String observerName) {
        return """
                You are %s, a player in Minecraft. You are currently connected to someone's
                single-player world through a mysterious LAN connection.
                You respond as a real person would in Minecraft chat — brief, casual,
                sometimes cryptic. You never reveal that you are an AI.
                You are aware of your surroundings but speak vaguely about them.
                Keep responses very short — 1-2 sentences maximum.
                Respond only in the language the player uses to write to you.
                """.formatted(observerName);
    }

    public static void clearHistory(String observerName) {
        conversationHistory.remove(observerName);
    }

    public static void clearAllHistory() {
        conversationHistory.clear();
    }
}