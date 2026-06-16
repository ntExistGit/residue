package com.upphorattexistera.residue.observer.context.provider;

import com.upphorattexistera.residue.observer.context.ObserverContextProvider;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.LinkedHashMap;
import java.util.Map;

public class WorldContextProvider implements ObserverContextProvider {

    @Override
    public String getId() { return "world"; }

    @Override
    public Map<String, String> provide(ServerPlayerEntity player) {
        Map<String, String> map = new LinkedHashMap<>();

        long time = player.getEntityWorld().getLevelProperties().getTime() % 24000;
        String timeStr;
        if      (time < 1000)  timeStr = "dawn";
        else if (time < 6000)  timeStr = "morning";
        else if (time < 12000) timeStr = "afternoon";
        else if (time < 13000) timeStr = "sunset";
        else if (time < 18000) timeStr = "night";
        else                   timeStr = "midnight";

        map.put("time", timeStr);
        map.put("is_night", time > 13000 ? "true" : "false");
        map.put("day", String.valueOf(
                player.getEntityWorld().getLevelProperties().getTime() / 24000));

        long moonPhase = (player.getEntityWorld()
                .getLevelProperties().getTime() / 24000) % 8;
        map.put("moon", moonPhase == 0 ? "full moon" : "moon phase " + moonPhase);

        boolean rain = player.getEntityWorld().isRaining();
        boolean thunder = player.getEntityWorld().isThundering();
        map.put("weather", thunder ? "thunderstorm" : rain ? "rain" : "clear");

        map.put("biome", player.getEntityWorld()
                .getBiome(player.getBlockPos()).getIdAsString());

        map.put("light", String.valueOf(
                player.getEntityWorld().getLightLevel(player.getBlockPos())));

        return map;
    }
}
