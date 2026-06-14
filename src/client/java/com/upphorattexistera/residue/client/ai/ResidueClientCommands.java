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

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // Перехватываем /msg, /tell, /w на клиенте
            var msgNode = literal("msg")
                    .then(argument("target", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                ResidueClientState.getObservers().forEach(entry ->
                                        builder.suggest(entry.name()));
                                return builder.buildFuture();
                            })
                            .then(argument("message", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        String target = StringArgumentType.getString(context, "target");
                                        String message = StringArgumentType.getString(context, "message");

                                        boolean isObserver = ResidueClientState.getObservers()
                                                .stream()
                                                .anyMatch(e -> e.name().equalsIgnoreCase(target));

                                        if (isObserver) {
                                            String observerName = ResidueClientState.getObservers()
                                                    .stream()
                                                    .filter(e -> e.name().equalsIgnoreCase(target))
                                                    .findFirst()
                                                    .map(ObserverListPacket.ObserverEntry::name)
                                                    .orElse(target);

                                            MinecraftClient client = context.getSource().getClient();
                                            client.execute(() -> {
                                                if (client.player != null) {
                                                    MessageType.Parameters params = MessageType.params(MessageType.MSG_COMMAND_OUTGOING, client.player)
                                                            .withTargetName(Text.literal(observerName));

                                                    Text formattedMessage = params.applyChatDecoration(Text.literal(message));

                                                    client.player.sendMessage(formattedMessage);
                                                }
                                            });

                                            ClientPlayNetworking.send(
                                                    new ObserverMessageRequestPacket.Payload(observerName, message));

                                            return 1;
                                        }

                                        throw EntityArgumentType.PLAYER_NOT_FOUND_EXCEPTION.create();
                                    })
                            )
                    );

            dispatcher.register(msgNode);
            dispatcher.register(literal("tell").redirect(dispatcher.getRoot().getChild("msg")));
            dispatcher.register(literal("w").redirect(dispatcher.getRoot().getChild("msg")));
        });
    }

    private static void registerPacketHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(
                ObserverMessagePacket.ID,
                (payload, context) -> {
                    String observerName = payload.observerName();
                    String playerMessage = payload.playerMessage();

                    Thread.ofVirtual().name("residue-observer-reply").start(() -> {
                        String reply = ChatAI.askAsObserver(observerName, playerMessage);

                        context.client().execute(() -> {
                            if (context.client().player == null) return;

                            if (reply != null) {
                                var registryManager = Objects.requireNonNull(context.client().world).getRegistryManager();

                                MessageType.Parameters params = MessageType.params(
                                        MessageType.MSG_COMMAND_INCOMING,
                                        registryManager,
                                        Text.literal(observerName)
                                );

                                Text formattedReply = params.applyChatDecoration(Text.literal(reply));
                                Objects.requireNonNull(context.client().player).sendMessage(formattedReply);
                            }
                        });
                    });
                }
        );
    }
}