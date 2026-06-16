package com.upphorattexistera.residue.observer.context;

import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;

public class ObserverContextRegistry {

    private static final List<ObserverContextProvider> providers = new ArrayList<>();

    public static void register(ObserverContextProvider provider) {
        providers.add(provider);
    }

    /**
     * Собирает все плейсхолдеры от всех провайдеров.
     */
    public static Map<String, String> collect(ServerPlayerEntity player) {
        Map<String, String> result = new LinkedHashMap<>();
        for (ObserverContextProvider provider : providers) {
            try {
                result.putAll(provider.provide(player));
            } catch (Exception e) {
                // один провайдер не должен ломать всё
            }
        }
        return result;
    }

    /**
     * Подставляет все плейсхолдеры в шаблон.
     */
    public static String inject(String template, ServerPlayerEntity player) {
        Map<String, String> placeholders = collect(player);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            template = template.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return template;
    }
}