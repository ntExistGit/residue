package com.upphorattexistera.residuemod.event.events;

import com.upphorattexistera.residuemod.WorldState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class DistantTorchEvent {

    private static final Map<UUID, FakeTorch> torches = new HashMap<>();

    public static void tick(MinecraftServer server) {

        if (WorldState.activeObserver == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {

            ServerLevel level = player.level();

            if (Math.random() < 0.0005) {
                spawnTorch(level, player);
            }
        }

        updateTorches(server);
    }

    private static void spawnTorch(ServerLevel level, ServerPlayer player) {

        Vec3 look = player.getLookAngle();

        double distance = 120 + Math.random() * 80;

        Vec3 pos = player.position().add(
                look.x * distance,
                2,
                look.z * distance
        );

        ItemEntity torch = new ItemEntity(
                level,
                pos.x,
                pos.y,
                pos.z,
                Items.TORCH.getDefaultInstance()
        );

        torch.setNoGravity(true);
        level.addFreshEntity(torch);

        torches.put(
                torch.getUUID(),
                new FakeTorch(level.dimension().identifier().toString())
        );
    }

    private static void updateTorches(MinecraftServer server) {

        Iterator<Map.Entry<UUID, FakeTorch>> it = torches.entrySet().iterator();

        while (it.hasNext()) {

            Map.Entry<UUID, FakeTorch> entry = it.next();
            FakeTorch data = entry.getValue();

            data.life++;

            ServerLevel level = getLevel(server, data.dimension);
            if (level == null) {
                it.remove();
                continue;
            }

            ItemEntity entity = findEntity(level, entry.getKey());

            if (data.life > 600 || entity == null) {
                if (entity != null) {
                    entity.discard();
                }
                it.remove();
                continue;
            }

            for (ServerPlayer player : level.players()) {
                if (player.distanceTo(entity) < 6) {
                    entity.discard();
                    it.remove();
                    break;
                }
            }
        }
    }

    private static ServerLevel getLevel(MinecraftServer server, String dimensionId) {

        ResourceKey<Level> key = ResourceKey.create(
                Registries.DIMENSION,
                Identifier.parse(dimensionId)
        );

        return server.getLevel(key);
    }

    private static ItemEntity findEntity(ServerLevel level, UUID id) {

        if (level.getEntityInAnyDimension(id) instanceof ItemEntity item) {
            return item;
        }

        return null;
    }

    private static class FakeTorch {

        String dimension;
        int life = 0;

        FakeTorch(String dimension) {
            this.dimension = dimension;
        }
    }
}
