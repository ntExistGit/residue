package com.upphorattexistera.residue.event.events;

import com.upphorattexistera.residue.WorldState;
import com.upphorattexistera.residue.config.ResidueConfig;
import com.upphorattexistera.residue.network.FakeLanPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;

import java.util.Random;

public class FakeLanEvent {

    private static final int TICKS_PER_SECOND = 20;
    private static final int TICKS_PER_MINUTE = TICKS_PER_SECOND * 60;
    private static final Random RANDOM = new Random();

    private static long triggerAtTick = -1;
    private static boolean triggered = false;

    public static boolean isActive() {
        return triggered;
    }

    public static void reset() {
        triggered = false;
        triggerAtTick = -1;
    }

    public static void init() {
        ResidueConfig cfg = ResidueConfig.INSTANCE;
        int range = cfg.fakeLanMaxMinutes - cfg.fakeLanMinMinutes;
        int delayMinutes = cfg.fakeLanMinMinutes + (range > 0 ? RANDOM.nextInt(range) : 0);
        triggerAtTick = WorldState.ticks + (long) delayMinutes * TICKS_PER_MINUTE;
    }

    public static void tick(MinecraftServer server) {

        if (!ResidueConfig.INSTANCE.enableFakeLanEvent) return;
        if (triggered) return;
        if (triggerAtTick < 0) return;

        if (WorldState.ticks >= triggerAtTick) {
            triggered = true;

            int port = 25000 + RANDOM.nextInt(10000);

            Text portText = Texts.bracketedCopyable(String.valueOf(port));

            server.getPlayerManager().broadcast(
                    Text.translatable("commands.publish.started", portText),
                    false
            );
            server.getPlayerManager().broadcast(
                    Text.translatable("message.voicechat.server_port", portText),
                    false
            );

            FakeLanPacket.sendTrigger(server);
        }
    }

    public static void forceTrigger(MinecraftServer server) {
        triggered = true;
        int port = 25000 + RANDOM.nextInt(10000);
        Text portText = net.minecraft.text.Texts.bracketedCopyable(String.valueOf(port));
        server.getPlayerManager().broadcast(
                Text.translatable("commands.publish.started", portText),
                false
        );
        FakeLanPacket.sendTrigger(server);
    }
}