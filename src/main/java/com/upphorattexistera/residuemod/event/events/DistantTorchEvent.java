package com.upphorattexistera.residuemod.event.events;

import com.upphorattexistera.residuemod.WorldState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class DistantTorchEvent {

    private static final Map<UUID, FakeTorch> torches = new HashMap<>();

    public static void tick(MinecraftServer server) {

        if (WorldState.activeObserver == null) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

            ServerWorld world = player.getEntityWorld();

            if (Math.random() < 0.0005) {
                spawnTorch(world, player);
            }
        }

        updateTorches(server);
    }

    private static void spawnTorch(ServerWorld world, ServerPlayerEntity player) {

        Vec3d look = player.getRotationVector();

        double distance = 120 + Math.random() * 80;

        Vec3d pos = player.getEntityPos().add(
                look.x * distance,
                2,
                look.z * distance
        );

        ItemEntity torch = new ItemEntity(
                world,
                pos.x,
                pos.y,
                pos.z,
                Items.TORCH.getDefaultStack(),
                0,
                0,
                0
        );

        torch.setNoGravity(true);
        world.spawnEntity(torch);

        torches.put(
                torch.getUuid(),
                new FakeTorch(world.getRegistryKey().getValue().toString())
        );
    }

    private static void updateTorches(MinecraftServer server) {

        Iterator<Map.Entry<UUID, FakeTorch>> it = torches.entrySet().iterator();

        while (it.hasNext()) {

            Map.Entry<UUID, FakeTorch> entry = it.next();
            FakeTorch data = entry.getValue();

            data.life++;

            ServerWorld world = getWorld(server, data.dimension);
            if (world == null) {
                it.remove();
                continue;
            }

            ItemEntity entity = findEntity(world, entry.getKey());

            if (data.life > 600 || entity == null) {
                if (entity != null) {
                    entity.discard();
                }
                it.remove();
                continue;
            }

            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.squaredDistanceTo(entity) < 36) {
                    entity.discard();
                    it.remove();
                    break;
                }
            }
        }
    }

    private static ServerWorld getWorld(MinecraftServer server, String dimensionId) {

        RegistryKey<World> key = RegistryKey.of(
                RegistryKeys.WORLD,
                Identifier.of(dimensionId)
        );

        return server.getWorld(key);
    }

    private static ItemEntity findEntity(ServerWorld world, UUID id) {

        if (world.getEntityAnyDimension(id) instanceof ItemEntity item) {
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
