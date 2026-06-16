package com.upphorattexistera.residue.observer.context.provider;

import com.upphorattexistera.residue.observer.context.ObserverContextProvider;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HealthContextProvider implements ObserverContextProvider {

    @Override
    public String getId() { return "health"; }

    @Override
    public Map<String, String> provide(ServerPlayerEntity player) {
        Map<String, String> map = new LinkedHashMap<>();
        float health = player.getHealth();
        float max = player.getMaxHealth();
        int pct = (int)(health / max * 100);

        map.put("health", String.valueOf((int) health));
        map.put("max_health", String.valueOf((int) max));
        map.put("health_pct", pct + "%");
        map.put("health_state",
                pct < 25 ? "critically injured" :
                        pct < 50 ? "injured" :
                                pct < 75 ? "slightly hurt" : "healthy");

        // Активные эффекты
        List<String> effects = new ArrayList<>();
        player.getStatusEffects().forEach(e ->
                effects.add(e.getEffectType().value()
                        .getName().getString()));
        map.put("effects", effects.isEmpty() ? "none" : String.join(", ", effects));

        map.put("food", String.valueOf(player.getHungerManager().getFoodLevel()));
        map.put("experience_level", String.valueOf(player.experienceLevel));

        return map;
    }
}
