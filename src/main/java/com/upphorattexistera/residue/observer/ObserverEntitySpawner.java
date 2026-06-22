package com.upphorattexistera.residue.observer;

import com.upphorattexistera.residue.Residue;
import com.upphorattexistera.residue.entity.ObserverEntity;
import com.upphorattexistera.residue.entity.ObserverEntityType;
import com.upphorattexistera.residue.memory.MemoryStage;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.*;

public class ObserverEntitySpawner {

    private static final Map<UUID, ObserverEntity> activeEntities = new HashMap<>();
    private static final Random RANDOM = new Random();

    public static void spawnForObserver(MinecraftServer server, Observer observer) {
        if (activeEntities.containsKey(observer.getUuid())) return;

        ServerPlayerEntity player = server.getPlayerManager()
                .getPlayerList().stream().findFirst().orElse(null);
        if (player == null) return;

        ServerWorld world = (ServerWorld) player.getEntityWorld();

        ObserverStageConfig stageConfig = ObserverStageConfig.forStage(MemoryStage.getCurrentStage());
        BlockPos spawnPos = findSurfacePos(world, player.getBlockPos(),
                stageConfig.minSpawnDistance(), stageConfig.maxSpawnDistance());

        if (spawnPos == null) {
            Residue.LOGGER.warn("[Residue] Could not find dry surface pos for observer: {}",
                    observer.getName());
            return;
        }

        ObserverEntity entity = ObserverEntityType.OBSERVER.create(world, SpawnReason.NATURAL);
        if (entity == null) return;

        entity.setObserverUuid(observer.getUuid());
        entity.setObserverName(observer.getName());

        entity.refreshPositionAndAngles(
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                RANDOM.nextFloat() * 360,
                0
        );
        world.spawnEntity(entity);

        activeEntities.put(observer.getUuid(), entity);
        Residue.LOGGER.debug("[Residue] Observer entity spawned: {} at {}",
                observer.getName(), spawnPos);
    }

    public static void despawnForObserver(UUID observerUuid) {
        ObserverEntity entity = activeEntities.remove(observerUuid);
        if (entity != null && !entity.isRemoved()) {
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

    public static Optional<ObserverEntity> getActive(UUID observerUuid) {
        return Optional.ofNullable(activeEntities.get(observerUuid));
    }

    /**
     * "Телепортирует" обсервера — ищет новую точку спавна по текущим параметрам
     * стейджа относительно игрока и переставляет туда существующую сущность,
     * не создавая новую и не прерывая сессию.
     * Используется системой видимости (watch/critical distance).
     *
     * @return true если телепортация удалась, false если не нашли подходящую точку
     *         (сущность в этом случае остаётся на месте)
     */
    public static boolean teleportObserver(ServerWorld world, ObserverEntity entity, BlockPos playerPos) {
        ObserverStageConfig stageConfig = ObserverStageConfig.forStage(MemoryStage.getCurrentStage());

        BlockPos newPos = findSurfacePos(world, playerPos,
                stageConfig.minSpawnDistance(), stageConfig.maxSpawnDistance());

        if (newPos == null) return false;

        entity.refreshPositionAndAngles(
                newPos.getX() + 0.5,
                newPos.getY(),
                newPos.getZ() + 0.5,
                RANDOM.nextFloat() * 360,
                0
        );
        // Сбрасываем скорость, чтобы не было "проскальзывания" после телепорта
        entity.setVelocity(0, 0, 0);

        Residue.LOGGER.debug("[Residue] Observer teleported: {} -> {}",
                entity.getObserverName(), newPos);
        return true;
    }

    /**
     * Ищет сухую (не водную) поверхность на дистанции [minDist, maxDist]
     * вокруг указанной позиции. Вода и лодки полностью исключены из логики спавна.
     */
    private static BlockPos findSurfacePos(ServerWorld world, BlockPos center,
                                           double minDist, double maxDist) {
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double distance = minDist + RANDOM.nextDouble() * (maxDist - minDist);
            int x = (int) (center.getX() + Math.cos(angle) * distance);
            int z = (int) (center.getZ() + Math.sin(angle) * distance);

            BlockPos surface = world.getTopPosition(
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(x, 0, z));

            if (surface.getY() <= world.getBottomY() + 5) continue;

            boolean isWater = !world.getFluidState(surface).isEmpty()
                    || !world.getFluidState(surface.down()).isEmpty();
            if (isWater) continue;

            return surface;
        }
        return null;
    }
}