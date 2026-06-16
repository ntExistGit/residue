package com.upphorattexistera.residue.observer.context.provider;

import com.upphorattexistera.residue.observer.context.ObserverContextProvider;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStateContextProvider implements ObserverContextProvider {

    // idle tracking: uuid → последний тик движения
    private static final Map<UUID, Long> lastMoveTick = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> lastPos = new ConcurrentHashMap<>();

    @Override
    public String getId() { return "player_state"; }

    @Override
    public Map<String, String> provide(ServerPlayerEntity player) {
        Map<String, String> map = new LinkedHashMap<>();

        // Idle
        BlockPos current = player.getBlockPos();
        BlockPos last = lastPos.get(player.getUuid());
        long now = player.getEntityWorld().getTime();

        if (last != null && last.equals(current)) {
            long idleTicks = now - lastMoveTick.getOrDefault(player.getUuid(), now);
            map.put("idle_seconds", String.valueOf(idleTicks / 20));
            map.put("is_idle", idleTicks > 100 ? "true" : "false");
        } else {
            lastMoveTick.put(player.getUuid(), now);
            lastPos.put(player.getUuid(), current);
            map.put("idle_seconds", "0");
            map.put("is_idle", "false");
        }

        map.put("is_sneaking", player.isSneaking() ? "true" : "false");
        map.put("is_sprinting", player.isSprinting() ? "true" : "false");
        map.put("is_swimming", player.isSwimming() ? "true" : "false");
        map.put("is_flying", player.getAbilities().flying ? "true" : "false");
        map.put("player", player.getName().getString());

        return map;
    }

    public static void reset() {
        lastMoveTick.clear();
        lastPos.clear();
    }
}
