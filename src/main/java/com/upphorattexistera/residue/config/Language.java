package com.upphorattexistera.residue.config;

public enum Language {
    ENGLISH("English", "english", "Communicate only in English.", "en"),
    RUSSIAN("Русский", "russian", "Общайся исключительно на русском языке.", "ru"),
    CHINESE("中文", "chinese", "只能使用简体中文进行交流。", "zh"),
    SPANISH("Español", "spanish", "Comunícate únicamente en español.", "es"),
    FRENCH("Français", "french", "Communique uniquement en français.", "fr"),
    GERMAN("Deutsch", "german", "Kommuniziere ausschließlich auf Deutsch.", "de"),
    ITALIAN("Italiano", "italian", "Comunica solo in italiano.", "it"),
    PORTUGUESE("Português", "portuguese", "Comunique-se apenas em português.", "pt"),
    JAPANESE("日本語", "japanese", "日本語のみで応答してください。", "ja"),
    KOREAN("한국어", "korean", "한국어로만 응답하세요。", "ko"),
    ARABIC("العربية", "arabic", "تواصل باللغة العربية فقط.", "ar"),
    TURKISH("Türkçe", "turkish", "Sadece Türkçe iletişim kur.", "tr"),
    POLISH("Polski", "polish", "Komunikuj się wyłącznie w języku polskim.", "pl"),
    UKRAINIAN("Українська", "ukrainian", "Спілкуйся виключно українською мовою.", "uk");

    public final String displayName;
    public final String id;
    private final String prompt;
    public final String ttsCode;

    Language(String displayName, String id, String prompt, String ttsCode) {
        this.displayName = displayName;
        this.id = id;
        this.prompt = prompt;
        this.ttsCode = ttsCode;
    }

    public String selectLanguage() {
        return this.prompt;
    }

    public static Language byId(String id) {
        for (Language lang : values()) {
            if (lang.id.equalsIgnoreCase(id)) {
                return lang;
            }
        }
        return ENGLISH;
    }
}