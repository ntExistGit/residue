package com.upphorattexistera.residue.observer;

import com.upphorattexistera.residue.Residue;
import com.upphorattexistera.residue.entity.ObserverEntity;
import com.upphorattexistera.residue.entity.ObserverEntityType;
import com.upphorattexistera.residue.memory.MemoryStage;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnReason;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.*;

public class ObserverEntitySpawner {

    private static final Map<UUID, ObserverEntity> activeEntities = new HashMap<>();
    private static final Random RANDOM = new Random();

    /** Сколько блоков вверх/вниз от уровня игрока ищем подходящую "природную" точку под землёй. */
    private static final int CAVE_VERTICAL_SEARCH_RANGE = 6;

    public static void spawnForObserver(MinecraftServer server, Observer observer) {
        if (activeEntities.containsKey(observer.getUuid())) return;

        ServerPlayerEntity player = server.getPlayerManager()
                .getPlayerList().stream().findFirst().orElse(null);
        if (player == null) return;

        ServerWorld world = (ServerWorld) player.getEntityWorld();

        ObserverStageConfig stageConfig = ObserverStageConfig.forStage(MemoryStage.getCurrentStage());
        BlockPos spawnPos = findSpawnPos(world, player,
                stageConfig.minSpawnDistance(), stageConfig.maxSpawnDistance());

        if (spawnPos == null) {
            Residue.LOGGER.warn("[Residue] Could not find valid spawn pos for observer: {}",
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
     */
    public static boolean teleportObserver(ServerWorld world, ObserverEntity entity, ServerPlayerEntity player) {
        ObserverStageConfig stageConfig = ObserverStageConfig.forStage(MemoryStage.getCurrentStage());

        BlockPos newPos = findSpawnPos(world, player,
                stageConfig.minSpawnDistance(), stageConfig.maxSpawnDistance());

        if (newPos == null) return false;

        entity.refreshPositionAndAngles(
                newPos.getX() + 0.5,
                newPos.getY(),
                newPos.getZ() + 0.5,
                RANDOM.nextFloat() * 360,
                0
        );
        entity.setVelocity(0, 0, 0);

        Residue.LOGGER.debug("[Residue] Observer teleported: {} -> {}",
                entity.getObserverName(), newPos);
        return true;
    }

    // ----------------------------------------------------------------
    // Поиск точки спавна
    // ----------------------------------------------------------------

    /**
     * Выбирает стратегию поиска в зависимости от того, находится ли
     * игрок под землёй (есть solid-перекрытие над головой) или на
     * открытом небе.
     */
    private static BlockPos findSpawnPos(ServerWorld world, ServerPlayerEntity player,
                                         double minDist, double maxDist) {
        if (isUnderground(world, player)) {
            BlockPos cavePos = findCaveSurfacePos(world, player.getBlockPos(), minDist, maxDist);
            if (cavePos != null) return cavePos;
            return null;
        }
        return findSurfacePos(world, player.getBlockPos(), minDist, maxDist);
    }

    private static boolean isUnderground(ServerWorld world, ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        for (int dy = 1; dy <= 5; dy++) {
            if (world.getBlockState(pos.up(dy)).isSolidBlock(world, pos.up(dy))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ищет "природную" (не дерево, не постройка, не вода) поверхность на
     * дистанции [minDist, maxDist] вокруг указанной позиции, используя
     * хитмап поверхности мира. Подходит только для игрока на открытом небе —
     * для пещер хитмап всегда вернёт точку на поверхности мира, а не рядом
     * с игроком, поэтому здесь не используется при isUnderground == true.
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
            if (!isNaturalGround(world, surface)) continue;
            if (isWaterAt(world, surface)) continue;

            return surface;
        }
        return null;
    }

    private static BlockPos findCaveSurfacePos(ServerWorld world, BlockPos center,
                                               double minDist, double maxDist) {
        for (int attempt = 0; attempt < 30; attempt++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double distance = minDist + RANDOM.nextDouble() * (maxDist - minDist);
            int x = (int) (center.getX() + Math.cos(angle) * distance);
            int z = (int) (center.getZ() + Math.sin(angle) * distance);

            BlockPos candidate = findStandablePosNear(world, x, center.getY(), z);
            if (candidate == null) continue;
            if (isWaterAt(world, candidate)) continue;

            return candidate;
        }
        return null;
    }

    private static BlockPos findStandablePosNear(ServerWorld world, int x, int baseY, int z) {
        for (int dy = 0; dy <= CAVE_VERTICAL_SEARCH_RANGE; dy++) {
            for (int sign : new int[]{1, -1}) {
                if (dy == 0 && sign == -1) continue;

                int y = baseY + dy * sign;
                int worldTopY = world.getBottomY() + world.getHeight();
                if (y <= world.getBottomY() + 1 || y >= worldTopY - 2) continue;

                BlockPos pos = new BlockPos(x, y, z);
                BlockPos below = pos.down();

                boolean airHere = world.getBlockState(pos).isAir();
                boolean headroomAbove = world.getBlockState(pos.up()).isAir();
                boolean solidFloor = isNaturalGround(world, below);

                if (airHere && headroomAbove && solidFloor) {
                    return pos;
                }
            }
        }
        return null;
    }

    private static boolean isWaterAt(ServerWorld world, BlockPos pos) {
        return !world.getFluidState(pos).isEmpty()
                || !world.getFluidState(pos.down()).isEmpty();
    }

    private static boolean isNaturalGround(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!state.isSolidBlock(world, pos)) return false;

        if (state.isIn(BlockTags.LOGS)
                || state.isIn(BlockTags.LEAVES)
                || state.isIn(BlockTags.PLANKS)
                || state.isIn(BlockTags.FENCES)
                || state.isIn(BlockTags.SLABS)
                || state.isIn(BlockTags.STAIRS))
            return false;

        return state.isIn(BlockTags.GRASS_BLOCKS)
                || state.isIn(BlockTags.DIRT)
                || state.isIn(BlockTags.MUD)
                || state.isIn(BlockTags.BASE_STONE_OVERWORLD)
                || state.isIn(BlockTags.BASE_STONE_NETHER)
                || state.isIn(BlockTags.SAND)
                || state.isIn(BlockTags.SNOW)
                || state.isIn(BlockTags.ICE)
                || state.isIn(BlockTags.TERRACOTTA)
                || state.isIn(BlockTags.NYLIUM)
                || state.isIn(BlockTags.MOSS_BLOCKS)

                || state.isIn(ConventionalBlockTags.GRAVELS)
                || state.isIn(ConventionalBlockTags.END_STONES)

                || state.getBlock() == Blocks.CALCITE
                || state.getBlock() == Blocks.CLAY
                || state.getBlock() == Blocks.ANDESITE
                || state.getBlock() == Blocks.DIORITE
                || state.getBlock() == Blocks.GRANITE
                || state.getBlock() == Blocks.TUFF
                || state.getBlock() == Blocks.MUDDY_MANGROVE_ROOTS;
    }
}