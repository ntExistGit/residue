package com.upphorattexistera.residue.network;

import com.upphorattexistera.residue.config.LLMLanguage;
import com.upphorattexistera.residue.config.ResidueConfig;
import com.upphorattexistera.residue.memory.MemoryManager;
import com.upphorattexistera.residue.observer.ObserverSessionManager;
import com.upphorattexistera.residue.observer.persona.ObserverAssignment;
import com.upphorattexistera.residue.observer.persona.ObserverDataStore;
import com.upphorattexistera.residue.observer.persona.ObserverPersona;
import com.upphorattexistera.residue.observer.persona.ObserverPersonaLoader;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ObserverMessageRequestPacket {

    public static final CustomPayload.Id<Payload> ID =
            new CustomPayload.Id<>(Identifier.of("residue", "observer_message_request"));

    /**
     * @param isPublic true  — запрос из общего чата
     *                 false — запрос из /msg (/tell /w)
     */
    public record Payload(String observerName, String playerMessage, boolean isPublic)
            implements CustomPayload {

        public static final PacketCodec<RegistryByteBuf, Payload> CODEC =
                PacketCodec.of(
                        (value, buf) -> {
                            buf.writeString(value.observerName());
                            buf.writeString(value.playerMessage());
                            buf.writeBoolean(value.isPublic());
                        },
                        buf -> new Payload(
                                buf.readString(),
                                buf.readString(),
                                buf.readBoolean()
                        )
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(ID, Payload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            String observerName  = payload.observerName();
            String playerMessage = payload.playerMessage();
            boolean isPublic     = payload.isPublic();
            String playerName    = context.player().getName().getString();

            // Обсервер должен быть активен
            boolean isActive = ObserverSessionManager.getSessions().stream()
                    .anyMatch(s -> s.observer.getName().equalsIgnoreCase(observerName));
            if (!isActive) return;

            ObserverAssignment assignment = ObserverDataStore.get(observerName);
            if (assignment == null) return;

            ObserverPersona persona = ObserverPersonaLoader.getById(assignment.personaId);
            if (persona == null) return;

            int stage  = getStage(MemoryManager.getMemory(), ResidueConfig.INSTANCE.maxMemory);

            LLMLanguage currentLang = ResidueConfig.INSTANCE.llmLang;

            String prompt      = persona.buildPrompt(observerName, stage, playerName, currentLang);
            String historyJson = ObserverDataStore.getHistory(observerName).toString();

            ObserverMessagePacket.sendToPlayer(context.player(),
                    new ObserverMessagePacket.Payload(
                            observerName,
                            playerMessage,
                            prompt,
                            persona.temperature,
                            persona.maxTokens,
                            historyJson,
                            isPublic   // пробрасываем флаг дальше на клиент
                    ));
        });
    }

    private static int getStage(int memory, int max) {
        if (memory < max * 0.20) return 0;
        if (memory < max * 0.40) return 1;
        if (memory < max * 0.60) return 2;
        if (memory < max * 0.80) return 3;
        return 4;
    }
}