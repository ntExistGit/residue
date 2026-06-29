package com.upphorattexistera.residue.observer.persona;

import com.upphorattexistera.residue.config.Language;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ObserverPersona {

    public final int id;
    public final Set<String> types;
    public final double temperature;
    public final int maxTokens;
    public final Map<Integer, List<String>> stages;

    public ObserverPersona(int id, Set<String> types,
                           double temperature, int maxTokens,
                           Map<Integer, List<String>> stages) {
        this.id = id;
        this.types = types;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.stages = stages;
    }

    public String buildPrompt(String observerName, int stage, String playerName,
                              Language language, ObserverGender gender) {

        if (language == null) language = Language.ENGLISH;
        if (gender == null) gender = ObserverGender.AMBIGUOUS;

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

        prompt.append("\n\n[CRITICAL SYSTEM INSTRUCTION — GENDER]: ")
                .append(gender.instruction);

        prompt.append("\n\n[CRITICAL SYSTEM INSTRUCTION — LANGUAGE]: ")
                .append(language.selectLanguage());

        return prompt.toString();
    }
}