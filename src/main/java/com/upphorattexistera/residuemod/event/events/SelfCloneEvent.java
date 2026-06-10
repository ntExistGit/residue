package com.upphorattexistera.residuemod.event.events;

import com.upphorattexistera.residuemod.WorldState;
import com.upphorattexistera.residuemod.config.ResidueConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Random;

public class SelfCloneEvent {

    private static final int TICKS_PER_SECOND = 20;

    private static long nextTriggerTick = 0L;
    private static final Random RANDOM = new Random();

    public static void tick(MinecraftServer server) {

        if (!ResidueConfig.INSTANCE.enableSelfCloneEvent) {
            return;
        }

        long now = WorldState.ticks;

        if (now < nextTriggerTick) {
            return;
        }

        ServerPlayerEntity player = server.getPlayerManager().getPlayerList().stream()
                .findFirst()
                .orElse(null);

        if (player == null) {
            return;
        }

        spawnClone(player);

        nextTriggerTick = now + (long) ResidueConfig.INSTANCE.selfCloneCooldownSeconds * TICKS_PER_SECOND;
    }

    private static void spawnClone(ServerPlayerEntity player) {

        ServerWorld world = player.getEntityWorld();
        boolean isNight = world.getLevelProperties().getTime() % 24000 > 13000;

        // placeholder — в будущем заменить на настоящий визуальный двойник
        System.out.println(
                "[RESIDUE] Clone spotted near "
                        + player.getName().getString()
                        + (isNight ? " (carrying torch)" : "")
        );

        // TODO: заспавнить визуальную сущность с факелом в левой руке если isNight
    }
}