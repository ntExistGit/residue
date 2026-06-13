package com.upphorattexistera.residue.event.events;

import com.upphorattexistera.residue.Residue;
import com.upphorattexistera.residue.config.ResidueConfig;
import com.upphorattexistera.residue.observer.ObserverSessionManager;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class DistantTorchEvent {

    private static final Map<BlockPos, TorchData> torches = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static final int TICKS_PER_SECOND = 20;

    public static void tick(MinecraftServer server) {

        if (!ObserverSessionManager.hasObserver()) return;
        if (!ResidueConfig.INSTANCE.enableDistantTorchEvent) return;
        if (torches.size() >= ResidueConfig.INSTANCE.torchMaxActive) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            double chance = ResidueConfig.INSTANCE.torchSpawnChance / 100000.0;
            if (RANDOM.nextDouble() < chance) {
                trySpawnTorch(player.getEntityWorld(), player);
            }
        }

        updateTorches(server);
    }

    private static void trySpawnTorch(ServerWorld world, ServerPlayerEntity player) {

        ResidueConfig cfg = ResidueConfig.INSTANCE;

        double distance = cfg.torchMinDistance + RANDOM.nextDouble() * (cfg.torchMaxDistance - cfg.torchMinDistance);

        Vec3d look = player.getRotationVector();
        Vec3d origin = player.getEntityPos();

        Vec3d horizontal = origin.add(look.x * distance, 0, look.z * distance);

        BlockPos targetPos;

        if (isUnderground(world, player)) {
            targetPos = findCaveSurface(world, origin, look, distance);
        } else {
            BlockPos surface = world.getTopPosition(
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos((int) horizontal.x, 0, (int) horizontal.z)
            );
            targetPos = surface;
        }

        if (targetPos == null) return;

        if (!world.getBlockState(targetPos).isAir()) return;
        if (!world.getBlockState(targetPos.down()).isSolidBlock(world, targetPos.down())) return;

        TorchData data = new TorchData(
                world.getRegistryKey().getValue().toString(),
                cfg.torchDespawnSeconds * TICKS_PER_SECOND
        );

        world.setBlockState(targetPos, Blocks.TORCH.getDefaultState());
        torches.put(targetPos, data);

        Residue.LOGGER.info("[Residue] Torch spawned at x={} y={} z={} dim={}",
                targetPos.getX(), targetPos.getY(), targetPos.getZ(), data.dimension);
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

    private static BlockPos findCaveSurface(ServerWorld world, Vec3d origin, Vec3d look, double distance) {
        for (double d = distance * 0.8; d <= distance; d += 1.0) {
            BlockPos candidate = new BlockPos(
                    (int) (origin.x + look.x * d),
                    (int) (origin.y + look.y * d),
                    (int) (origin.z + look.z * d)
            );
            BlockPos below = candidate.down();
            if (world.getBlockState(candidate).isAir() &&
                    world.getBlockState(below).isSolidBlock(world, below)) {
                return candidate;
            }
        }
        return null;
    }

    private static void updateTorches(MinecraftServer server) {

        ResidueConfig cfg = ResidueConfig.INSTANCE;
        double disappearDistSq = cfg.torchDisappearDistance * cfg.torchDisappearDistance;

        Iterator<Map.Entry<BlockPos, TorchData>> it = torches.entrySet().iterator();

        while (it.hasNext()) {

            Map.Entry<BlockPos, TorchData> entry = it.next();
            BlockPos pos = entry.getKey();
            TorchData data = entry.getValue();

            data.lifeTicks++;

            ServerWorld world = getWorld(server, data.dimension);

            if (world == null) {
                it.remove();
                continue;
            }

            if (!world.getBlockState(pos).is(Blocks.TORCH)) {
                it.remove();
                continue;
            }

            if (data.lifeTicks >= data.maxLifeTicks) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
                it.remove();
                continue;
            }

            for (ServerPlayerEntity player : world.getPlayers()) {
                Vec3d torchVec = Vec3d.ofCenter(pos);
                if (player.getEntityPos().squaredDistanceTo(torchVec) < disappearDistSq) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
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

    private static class TorchData {
        String dimension;
        int lifeTicks = 0;
        int maxLifeTicks;

        TorchData(String dimension, int maxLifeTicks) {
            this.dimension = dimension;
            this.maxLifeTicks = maxLifeTicks;
        }
    }
}