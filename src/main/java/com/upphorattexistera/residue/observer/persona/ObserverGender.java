package com.upphorattexistera.residue.observer.persona;

import java.util.Random;

/**
 * Пол обсервера, выбирается случайно один раз при первом подключении
 * (ObserverDataStore.getOrCreate) и сохраняется на весь срок жизни
 * привязки. Используется как для фильтрации скинов (префикс файла),
 * так и для системной инструкции LLM.
 */
public enum ObserverGender {

    MALE("m",
            "You are male. Respond and refer to yourself accordingly — " +
                    "use a male name if one comes up, and male-coded language and tone."),

    FEMALE("f",
            "You are female. Respond and refer to yourself accordingly — " +
                    "use a female name if one comes up, and female-coded language and tone."),

    AMBIGUOUS("b",
            "Deliberately conceal your gender at all times. Never reveal, confirm, " +
                    "or deny whether you are male or female. Avoid gendered self-references, " +
                    "names, and pronouns that would make your gender obvious. If asked directly, deflect.");

    public final String filePrefix;
    public final String instruction;

    ObserverGender(String filePrefix, String instruction) {
        this.filePrefix = filePrefix;
        this.instruction = instruction;
    }

    public static ObserverGender random(Random random) {
        ObserverGender[] values = values();
        return values[random.nextInt(values.length)];
    }

    public static ObserverGender byId(String id) {
        if (id == null) return AMBIGUOUS;
        for (ObserverGender g : values()) {
            if (g.filePrefix.equalsIgnoreCase(id)) return g;
        }
        return AMBIGUOUS;
    }
}