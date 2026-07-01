package com.upphorattexistera.residue.config;

import java.util.List;
import java.util.Map;

/** Источник истины для имён голосов Qwen3-TTS CustomVoice и их пола. */
public final class TTSSpeakers {

    public static final List<String> ALL = List.of(
            "vivian", "serena", "ono_anna", "sohee",      // female
            "ryan", "dylan", "eric", "aiden", "uncle_fu"   // male
    );

    private static final Map<String, String> GENDER = Map.of(
            "vivian", "f", "serena", "f", "ono_anna", "f", "sohee", "f",
            "ryan", "m", "dylan", "m", "eric", "m", "aiden", "m", "uncle_fu", "m"
    );

    private TTSSpeakers() {}

    public static String genderOf(String speaker) {
        return GENDER.getOrDefault(speaker, "b");
    }

    public static List<String> forGender(String filePrefix, List<String> enabled) {
        return enabled.stream()
                .filter(s -> ALL.contains(s)) // защита от мусора в конфиге
                .filter(s -> filePrefix.equals("b") || filePrefix.equals(genderOf(s)))
                .toList();
    }
}