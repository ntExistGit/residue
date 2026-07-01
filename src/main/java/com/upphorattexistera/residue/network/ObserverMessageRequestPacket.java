package com.upphorattexistera.residue.network;

import com.upphorattexistera.residue.config.Language;
import com.upphorattexistera.residue.config.ResidueConfig;
import com.upphorattexistera.residue.memory.MemoryManager;
import com.upphorattexistera.residue.memory.MemoryStage;
import com.upphorattexistera.residue.observer.ObserverSessionManager;
import com.upphorattexistera.residue.observer.persona.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ObserverMessageRequestPacket {

    private static final int MAX_MESSAGE_LENGTH = 2000;

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
            String playerMessage = truncate(payload.playerMessage());
            boolean isPublic     = payload.isPublic();
            String playerName    = context.player().getName().getString();

            boolean isActive = ObserverSessionManager.getSessions().stream()
                    .anyMatch(s -> s.observer.getName().equalsIgnoreCase(observerName));
            if (!isActive) return;

            ObserverAssignment assignment = ObserverDataStore.get(observerName);
            if (assignment == null) return;

            ObserverPersona persona = ObserverPersonaLoader.getById(assignment.personaId);
            if (persona == null) return;

            int stage  = MemoryStage.getStage(MemoryManager.getMemory(), ResidueConfig.INSTANCE.maxMemory);

            Language currentLang = ResidueConfig.INSTANCE.language;

            ObserverGender gender = ObserverGender.byId(assignment.gender);

            String prompt = persona.buildPrompt(observerName, stage, playerName, currentLang, gender);
            String historyJson = ObserverDataStore.getHistory(observerName).toString();

            ObserverMessagePacket.sendToPlayer(context.player(),
                    new ObserverMessagePacket.Payload(
                            observerName,
                            playerMessage,
                            prompt,
                            persona.temperature,
                            persona.maxTokens,
                            historyJson,
                            isPublic
                    ));
        });
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > MAX_MESSAGE_LENGTH ? s.substring(0, MAX_MESSAGE_LENGTH) : s;
    }
}