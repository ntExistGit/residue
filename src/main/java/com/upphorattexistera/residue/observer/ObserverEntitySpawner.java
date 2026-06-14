package com.upphorattexistera.residue.observer;

import com.upphorattexistera.residue.Residue;
import com.upphorattexistera.residue.entity.ObserverEntity;
import com.upphorattexistera.residue.entity.ObserverEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.*;

public class ObserverEntitySpawner {

    private static final Map<UUID, ObserverEntity> activeEntities = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static final double MIN_DIST = 30;
    private static final double MAX_DIST = 70;

    private static final EntityType<? extends BoatEntity>[] BOAT_TYPES = new EntityType[]{
            EntityType.OAK_BOAT,
            EntityType.SPRUCE_BOAT,
            EntityType.BIRCH_BOAT,
            EntityType.JUNGLE_BOAT,
            EntityType.ACACIA_BOAT,
            EntityType.CHERRY_BOAT,
            EntityType.DARK_OAK_BOAT,
            EntityType.MANGROVE_BOAT,
            EntityType.BAMBOO_RAFT
    };

    public static void spawnForObserver(MinecraftServer server, Observer observer) {
        if (activeEntities.containsKey(observer.getUuid())) return;

        ServerPlayerEntity player = server.getPlayerManager()
                .getPlayerList().stream().findFirst().orElse(null);
        if (player == null) return;

        ServerWorld world = (ServerWorld) player.getEntityWorld();
        BlockPos spawnPos = findSurfacePos(world, player.getBlockPos());
        if (spawnPos == null) {
            Residue.LOGGER.warn("[Residue] Could not find surface pos for observer: {}", observer.getName());
            return;
        }

        ObserverEntity entity = ObserverEntityType.OBSERVER.create(world, SpawnReason.NATURAL);
        if (entity == null) return;

        entity.setObserverUuid(observer.getUuid());
        entity.setObserverName(observer.getName());

        boolean isWater = !world.getFluidState(spawnPos).isEmpty()
                || !world.getFluidState(spawnPos.down()).isEmpty();

        if (isWater) {
            EntityType<? extends BoatEntity> boatType = BOAT_TYPES[RANDOM.nextInt(BOAT_TYPES.length)];
            BoatEntity boat = boatType.create(world, SpawnReason.NATURAL);
            if (boat == null) return;

            boat.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5, 0, 0);
            world.spawnEntity(boat);
            entity.refreshPositionAndAngles(boat.getX(), boat.getY(), boat.getZ(), 0, 0);
            world.spawnEntity(entity);
            entity.startRiding(boat);
            entity.setAiDisabled(true);
        } else {
            entity.refreshPositionAndAngles(
                    spawnPos.getX() + 0.5,
                    spawnPos.getY(),
                    spawnPos.getZ() + 0.5,
                    RANDOM.nextFloat() * 360,
                    0
            );
            world.spawnEntity(entity);
        }

        activeEntities.put(observer.getUuid(), entity);
        Residue.LOGGER.debug("[Residue] Observer entity spawned: {} at {} (water={})",
                observer.getName(), spawnPos, isWater);
    }

    public static void despawnForObserver(UUID observerUuid) {
        ObserverEntity entity = activeEntities.remove(observerUuid);
        if (entity != null && !entity.isRemoved()) {
            if (entity.getVehicle() instanceof BoatEntity boat) {
                boat.discard();
            }
            entity.discard();
        }
    }

    public static void clearAll() {
        for (ObserverEntity entity : activeEntities.values()) {
            if (!entity.isRemoved()) {
                entity.discard();
            }
        }
        activeEntities.clear();
    }

    private static BlockPos findSurfacePos(ServerWorld world, BlockPos center) {
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double distance = MIN_DIST + RANDOM.nextDouble() * (MAX_DIST - MIN_DIST);
            int x = (int) (center.getX() + Math.cos(angle) * distance);
            int z = (int) (center.getZ() + Math.sin(angle) * distance);
            BlockPos surface = world.getTopPosition(
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(x, 0, z));
            if (surface.getY() > world.getBottomY() + 5) {
                return surface;
            }
        }
        return null;
    }
}