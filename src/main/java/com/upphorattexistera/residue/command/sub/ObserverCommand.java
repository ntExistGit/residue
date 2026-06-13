package com.upphorattexistera.residue.command.sub;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.upphorattexistera.residue.WorldState;
import com.upphorattexistera.residue.observer.Observer;
import com.upphorattexistera.residue.observer.ObserverConnectionEvent;
import com.upphorattexistera.residue.observer.ObserverManager;
import com.upphorattexistera.residue.observer.ObserverSessionManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ObserverCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("observer")
                .executes(ObserverCommand::listAllObservers)

                .then(CommandManager.literal("list")
                        .executes(ObserverCommand::listActiveSessions))

                .then(CommandManager.literal("connect")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .executes(ObserverCommand::connectObserver)))

                .then(CommandManager.literal("disconnect")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .executes(ObserverCommand::disconnectObserver)));
    }

    private static int listAllObservers(CommandContext<ServerCommandSource> context) {
        ObserverManager.getAll().forEach(observer -> context.getSource().sendFeedback(() -> Text.literal(
                observer.getName() +
                        " | weight=" + observer.getWeight() +
                        " | used=" + observer.isUsed()),
                false));
        return 1;
    }

    private static int listActiveSessions(CommandContext<ServerCommandSource> context) {
        var sessions = ObserverSessionManager.getSessions();

        if (sessions.isEmpty()) {
            context.getSource().sendFeedback(
                    () -> Text.literal("No active observers"), false);
            return 1;
        }

        sessions.forEach(s -> {
            long remaining = (s.disconnectAtTick - WorldState.ticks) / 20;
            context.getSource().sendFeedback(() -> Text.literal(
                    s.observer.getName() +
                            " | attention=" + s.attention +
                            " | disconnects in " + remaining + "s"),
                    false);
        });

        return 1;
    }

    private static int connectObserver(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");

        Observer target = ObserverManager.getAll().stream()
                .filter(o -> o.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);

        if (target == null) {
            context.getSource().sendFeedback(
                    () -> Text.literal("Observer not found: " + name), false);
            return 0;
        }

        ObserverConnectionEvent.forceConnect(context.getSource().getServer(), target);
        context.getSource().sendFeedback(
                () -> Text.literal("Connected: " + name), false);

        return 1;
    }

    private static int disconnectObserver(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");

        Observer target = ObserverSessionManager.getSessions().stream()
                .map(s -> s.observer)
                .filter(o -> o.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);

        if (target == null) {
            context.getSource().sendFeedback(
                    () -> Text.literal("Observer not active: " + name), false);
            return 0;
        }

        ObserverConnectionEvent.forceDisconnect(context.getSource().getServer(), target);
        context.getSource().sendFeedback(
                () -> Text.literal("Disconnected: " + name), false);

        return 1;
    }
}