package com.upphorattexistera.residue.client.ai;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.upphorattexistera.residue.client.ResidueClientState;
import com.upphorattexistera.residue.network.ObserverListPacket;
import com.upphorattexistera.residue.network.ObserverMessagePacket;
import com.upphorattexistera.residue.network.ObserverMessageRequestPacket;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.message.MessageType;
import net.minecraft.text.Text;

import java.util.Objects;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class ResidueClientCommands {

    public static void register() {
        registerPacketHandlers();
        registerCommands();
        registerLlmCommands();
    }

    // ----------------------------------------------------------------
    // Команды /residueai start | stop | toggle | status
    // ----------------------------------------------------------------

    private static void registerLlmCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("residueai")
                    .then(literal("start").executes(ctx -> {
                        LLMServerController.start(null);
                        return 1;
                    }))
                    .then(literal("stop").executes(ctx -> {
                        LLMServerController.stop(null);
                        return 1;
                    }))
                    .then(literal("toggle").executes(ctx -> {
                        LLMServerController.toggle(null);
                        return 1;
                    }))
                    .then(literal("status").executes(ctx -> {
                        boolean running = LLMServerManager.getInstance().isRunning();
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal(
                                    running
                                            ? "§a[Residue] AI server is running."
                                            : "§c[Residue] AI server is stopped."));
                        }
                        return 1;
                    }))
            );
        });
    }

    // ----------------------------------------------------------------
    // Команды /msg, /tell, /w
    // ----------------------------------------------------------------

    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            var msgNode = literal("msg")
                    .then(argument("target", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                ResidueClientState.getObservers().forEach(e ->
                                        builder.suggest(e.name()));
                                return builder.buildFuture();
                            })
                            .then(argument("message", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        String target  = StringArgumentType.getString(context, "target");
                                        String message = StringArgumentType.getString(context, "message");

                                        boolean isObserver = ResidueClientState.getObservers().stream()
                                                .anyMatch(e -> e.name().equalsIgnoreCase(target));

                                        if (!isObserver) {
                                            throw EntityArgumentType.PLAYER_NOT_FOUND_EXCEPTION.create();
                                        }

                                        String observerName = ResidueClientState.getObservers().stream()
                                                .filter(e -> e.name().equalsIgnoreCase(target))
                                                .findFirst()
                                                .map(ObserverListPacket.ObserverEntry::name)
                                                .orElse(target);

                                        MinecraftClient client = context.getSource().getClient();

                                        client.execute(() -> {
                                            if (client.player == null) return;
                                            MessageType.Parameters params = MessageType
                                                    .params(MessageType.MSG_COMMAND_OUTGOING, client.player)
                                                    .withTargetName(Text.literal(observerName));
                                            client.player.sendMessage(
                                                    params.applyChatDecoration(Text.literal(message)));
                                        });

                                        // isPublic = false — личное сообщение
                                        ClientPlayNetworking.send(
                                                new ObserverMessageRequestPacket.Payload(
                                                        observerName, message, false));

                                        return 1;
                                    })
                            )
                    );

            dispatcher.register(msgNode);
            dispatcher.register(literal("tell").redirect(dispatcher.getRoot().getChild("msg")));
            dispatcher.register(literal("w").redirect(dispatcher.getRoot().getChild("msg")));
        });
    }

    // ----------------------------------------------------------------
    // Обработка входящего ответа от обсервера
    // ----------------------------------------------------------------

    private static void registerPacketHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(
                ObserverMessagePacket.ID,
                (payload, context) -> {
                    String  observerName  = payload.observerName();
                    String  playerMessage = payload.playerMessage();
                    String  systemPrompt  = payload.systemPrompt();
                    double  temperature   = payload.temperature();
                    int     maxTokens     = payload.maxTokens();
                    String  historyJson   = payload.historyJson();
                    boolean isPublic      = payload.isPublic();

                    Thread.ofVirtual().name("residue-observer-reply-" + observerName).start(() -> {
                        String reply = ChatAI.askAsObserver(
                                observerName, playerMessage,
                                systemPrompt, temperature, maxTokens, historyJson);

                        if (reply == null) return;

                        context.client().execute(() -> {
                            if (context.client().player == null) return;

                            if (isPublic) {
                                sendPublicReply(context.client(), observerName, reply);
                            } else {
                                sendPrivateReply(context.client(), observerName, reply);
                            }
                        });
                    });
                }
        );
    }

    // ----------------------------------------------------------------
    // Публичный ответ (общий чат)
    // ----------------------------------------------------------------

    private static void sendPublicReply(MinecraftClient client, String observerName, String reply) {
        var registryManager = Objects.requireNonNull(client.world).getRegistryManager();

        MessageType.Parameters params = MessageType.params(
                MessageType.CHAT,
                registryManager,
                Text.literal(observerName));

        client.player.sendMessage(params.applyChatDecoration(Text.literal(reply)));
    }

    // ----------------------------------------------------------------
    // Личный ответ (/msg)
    // ----------------------------------------------------------------

    private static void sendPrivateReply(MinecraftClient client, String observerName, String reply) {
        var registryManager = Objects.requireNonNull(client.world).getRegistryManager();

        MessageType.Parameters params = MessageType.params(
                MessageType.MSG_COMMAND_INCOMING,
                registryManager,
                Text.literal(observerName));

        client.player.sendMessage(params.applyChatDecoration(Text.literal(reply)));
    }
}