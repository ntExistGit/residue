package com.upphorattexistera.residue.command.sub;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.upphorattexistera.residue.network.ObserverMessagePacket;
import com.upphorattexistera.residue.observer.ObserverSessionManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class MessageCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("msg")
                .then(CommandManager.argument("target", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            // Предлагаем имена активных обсерверов
                            for (ObserverSessionManager.Session session
                                    : ObserverSessionManager.getSessions()) {
                                String name = session.observer.getName();
                                if (name.toLowerCase().startsWith(
                                        builder.getRemainingLowerCase())) {
                                    builder.suggest(name);
                                }
                            }
                            // Также предлагаем реальных игроков
                            ServerCommandSource source = context.getSource();
                            if (source.getServer() != null) {
                                for (ServerPlayerEntity player
                                        : source.getServer().getPlayerManager().getPlayerList()) {
                                    builder.suggest(player.getName().getString());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .then(CommandManager.argument("message",
                                        StringArgumentType.greedyString())
                                .executes(context -> {
                                    String target = StringArgumentType.getString(
                                            context, "target");
                                    String message = StringArgumentType.getString(
                                            context, "message");
                                    ServerCommandSource source = context.getSource();

                                    // Проверяем — активный ли это обсервер
                                    boolean isObserver = ObserverSessionManager.getSessions()
                                            .stream()
                                            .anyMatch(s -> s.observer.getName()
                                                    .equalsIgnoreCase(target));

                                    if (isObserver) {
                                        handleObserverMessage(source, target, message);
                                        return 1;
                                    }

                                    // Иначе — обычный игрок, ищем и пересылаем
                                    ServerPlayerEntity targetPlayer = source.getServer()
                                            .getPlayerManager().getPlayer(target);
                                    if (targetPlayer != null) {
                                        // Стандартное поведение через ванильный механизм
                                        targetPlayer.sendMessage(
                                                Text.literal("§7[" + source.getName()
                                                        + " -> me] §r" + message));
                                        source.sendMessage(
                                                Text.literal("§7[me -> " + target
                                                        + "] §r" + message));
                                    } else {
                                        source.sendMessage(Text.literal(
                                                "§cNo player found with name: " + target));
                                    }

                                    return 1;
                                })
                        )
                );
    }

    private static void handleObserverMessage(ServerCommandSource source,
                                              String observerName, String message) {
        ServerPlayerEntity player;
        try {
            player = source.getPlayer();
        } catch (Exception e) {
            return;
        }

        // Показываем игроку что его сообщение отправлено
        player.sendMessage(Text.literal(
                "§7[me -> " + observerName + "] §r" + message));

        // Запускаем LLM в отдельном потоке
        Thread.ofVirtual().name("residue-ai-" + observerName).start(() -> {
            // Ответ генерируется на клиенте через ChatAI
            // Отправляем событие клиенту через пакет
            source.getServer().execute(() -> {
                ObserverMessagePacket.sendToPlayer(player, observerName, message);
            });
        });
    }
}