package com.upphorattexistera.residuemod.event.events;

import com.upphorattexistera.residuemod.config.ResidueConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Random;

public class SelfCloneEvent {

    private static long nextTrigger = 0L;
    private static final Random RANDOM = new Random();

    public static void tick(MinecraftServer server) {

        if (!ResidueConfig.INSTANCE.enableSelfCloneEvent) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now < nextTrigger) {
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayers().stream()
                .findFirst()
                .orElse(null);

        if (player == null) {
            return;
        }

        spawnClone(player);

        nextTrigger =
                now +
                        ResidueConfig.INSTANCE.selfCloneCooldownSeconds * 1000L;
    }

    private static void spawnClone(ServerPlayer player) {

        // Пока только лог

        System.out.println(
                "[RESIDUE] Clone spotted near "
                        + player.getName().getString()
        );
    }
}