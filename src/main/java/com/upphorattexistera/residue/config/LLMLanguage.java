package com.upphorattexistera.residue.config;

public enum LLMLanguage {
    ENGLISH("English", "english", "Communicate only in English."),
    RUSSIAN("Русский", "russian", "Общайся исключительно на русском языке."),
    CHINESE("中文", "chinese", "只能使用简体中文进行交流。"),
    SPANISH("Español", "spanish", "Comunícate únicamente en español."),
    FRENCH("Français", "french", "Communique uniquement en français."),
    GERMAN("Deutsch", "german", "Kommuniziere ausschließlich auf Deutsch."),
    ITALIAN("Italiano", "italian", "Comunica solo in italiano."),
    PORTUGUESE("Português", "portuguese", "Comunique-se apenas em português."),
    JAPANESE("日本語", "japanese", "日本語のみで応答してください。"),
    KOREAN("한국어", "korean", "한국어로만 응답하세요."),
    ARABIC("العربية", "arabic", "تواصل باللغة العربية فقط."),
    TURKISH("Türkçe", "turkish", "Sadece Türkçe iletişim kur."),
    POLISH("Polski", "polish", "Komunikuj się wyłącznie w języku polskim."),
    UKRAINIAN("Українська", "ukrainian", "Спілкуйся виключно українською мовою.");

    public final String displayName;
    public final String id;
    private final String prompt;

    LLMLanguage(String displayName, String id, String prompt) {
        this.displayName = displayName;
        this.id = id;
        this.prompt = prompt;
    }

    public String selectLanguage() {
        return this.prompt;
    }

    public static LLMLanguage byId(String id) {
        for (LLMLanguage lang : values()) {
            if (lang.id.equalsIgnoreCase(id)) {
                return lang;
            }
        }
        return ENGLISH;
    }
}