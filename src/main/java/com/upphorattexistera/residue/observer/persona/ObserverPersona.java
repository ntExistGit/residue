package com.upphorattexistera.residue.observer.persona;

import com.upphorattexistera.residue.config.LLMLanguage; // <-- Добавляем импорт
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ObserverPersona {

    public final int id;
    public final String gender;
    public final Set<String> types;
    public final double temperature;
    public final int maxTokens;
    public final Map<Integer, List<String>> stages;

    public ObserverPersona(int id, String gender, Set<String> types,
                           double temperature, int maxTokens,
                           Map<Integer, List<String>> stages) {
        this.id = id;
        this.gender = gender;
        this.types = types;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.stages = stages;
    }

    // 1. Добавляем параметр LLMLanguage в сигнатуру метода
    public String buildPrompt(String observerName, int stage, String playerName, LLMLanguage language) {

        // Небольшая защита от null, если язык не передался
        if (language == null) {
            language = LLMLanguage.ENGLISH;
        }

        List<String> lines = stages.getOrDefault(stage, stages.get(0));
        if (lines == null || lines.isEmpty()) {
            return "You are " + observerName + ".";
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append(String.join("\n", lines).formatted(observerName));
        prompt.append("\nThe player's name is ").append(playerName).append(".");

        String globalRules = ObserverPersonaLoader.getGlobalRules();
        if (!globalRules.isBlank()) {
            prompt.append("\n\n# Global rules — always follow these:\n");
            prompt.append(globalRules);
        }

        // 2. --- НОВАЯ ЧАСТЬ: Инъекция языка в самом конце ---
        // Выделяем это как строгую системную инструкцию.
        prompt.append("\n\n[CRITICAL SYSTEM INSTRUCTION]: ")
                .append(language.selectLanguage());

        return prompt.toString();
    }
}