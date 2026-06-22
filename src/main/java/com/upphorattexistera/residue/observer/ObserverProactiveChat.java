package com.upphorattexistera.residue.observer;

import com.upphorattexistera.residue.WorldState;
import com.upphorattexistera.residue.config.LLMLanguage;
import com.upphorattexistera.residue.config.ResidueConfig;
import com.upphorattexistera.residue.memory.MemoryManager;
import com.upphorattexistera.residue.network.ObserverMessagePacket;
import com.upphorattexistera.residue.observer.context.ObserverContextRegistry;
import com.upphorattexistera.residue.observer.persona.*;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class ObserverProactiveChat {

    private static final Random RANDOM = new Random();

    private static final Map<String, Long> lastMessageTick  = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastCooldownEnd  = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // Tick
    // ----------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        if (WorldState.ticks % 20 != 0) return;
        if (!ObserverSessionManager.hasObserver()) return;

        int memory = MemoryManager.getMemory();
        int max    = ResidueConfig.INSTANCE.maxMemory;
        int stage  = getStage(memory, max);

        ServerPlayerEntity player = server.getPlayerManager()
                .getPlayerList().stream().findFirst().orElse(null);
        if (player == null) return;

        List<ObserverSessionManager.Session> sessions =
                new ArrayList<>(ObserverSessionManager.getSessions());
        Collections.shuffle(sessions);

        for (ObserverSessionManager.Session session : sessions) {
            String observerName = session.observer.getName();

            ObserverAssignment assignment = ObserverDataStore.get(observerName);
            if (assignment == null) continue;

            ObserverPersona persona = ObserverPersonaLoader.getById(assignment.personaId);
            if (persona == null || persona.types.isEmpty()) continue;

            long cooldownEnd = lastCooldownEnd.getOrDefault(observerName, 0L);
            if (WorldState.ticks < cooldownEnd) continue;

            List<String> typeIds = new ArrayList<>(persona.types);
            Collections.shuffle(typeIds);

            for (String typeId : typeIds) {
                ObserverType type = ObserverTypeLoader.getById(typeId);
                if (type == null) continue;

                double chancePerMinute = type.getChance(stage);
                double chancePerSecond = chancePerMinute / 100.0 / 60.0;
                if (RANDOM.nextDouble() >= chancePerSecond) continue;

                String contextKey = resolveContextKey(player, typeId);
                String rawContext  = type.getContext(contextKey);
                String context     = ObserverContextRegistry.inject(rawContext, player);

                LLMLanguage currentLang = ResidueConfig.INSTANCE.llmLang;

                String prompt      = persona.buildPrompt(observerName, stage,
                        player.getName().getString(), currentLang);
                String historyJson = ObserverDataStore.getHistory(observerName).toString();

                long nextCooldown = type.getRandomCooldownTicks(RANDOM);
                lastCooldownEnd.put(observerName, WorldState.ticks + nextCooldown);

                // Проактивные сообщения всегда идут в общий чат (isPublic = true)
                ObserverMessagePacket.sendToPlayer(player,
                        new ObserverMessagePacket.Payload(
                                observerName,
                                context,
                                prompt,
                                persona.temperature,
                                persona.maxTokens,
                                historyJson,
                                true  // isPublic
                        ));
                break;
            }
        }
    }

    // ----------------------------------------------------------------

    private static String resolveContextKey(ServerPlayerEntity player, String typeId) {
        boolean isNight       = player.getEntityWorld()
                .getLevelProperties().getTime() % 24000 > 13000;
        boolean lowHealth     = player.getHealth() < player.getMaxHealth() * 0.3f;
        boolean underground   = player.getBlockPos().getY() < 60;
        boolean emptyInventory = player.getInventory().getMainStacks()
                .stream().allMatch(ItemStack::isEmpty);

        if (lowHealth) return "low_health";
        if (typeId.equals("watcher") && underground) return "underground";
        if (isNight) return "night";
        if (typeId.equals("inventory") && emptyInventory) return "empty";
        return "default";
    }

    private static int getStage(int memory, int max) {
        if (memory < max * 0.20) return 0;
        if (memory < max * 0.40) return 1;
        if (memory < max * 0.60) return 2;
        if (memory < max * 0.80) return 3;
        return 4;
    }

    public static void reset() {
        lastMessageTick.clear();
        lastCooldownEnd.clear();
    }
}