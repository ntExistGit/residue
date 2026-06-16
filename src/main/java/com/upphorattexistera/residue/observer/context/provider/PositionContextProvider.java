package com.upphorattexistera.residue.observer.context.provider;

import com.upphorattexistera.residue.observer.context.ObserverContextProvider;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.LinkedHashMap;
import java.util.Map;

public class PositionContextProvider implements ObserverContextProvider {

    @Override
    public String getId() { return "position"; }

    @Override
    public Map<String, String> provide(ServerPlayerEntity player) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("x", String.valueOf(player.getBlockPos().getX()));
        map.put("y", String.valueOf(player.getBlockPos().getY()));
        map.put("z", String.valueOf(player.getBlockPos().getZ()));
        map.put("surface", player.getBlockPos().getY() < 60
                ? "underground" : "on the surface");
        return map;
    }
}