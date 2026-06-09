package com.upphorattexistera.residuemod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.upphorattexistera.residuemod.memory.MemoryManager;
import com.upphorattexistera.residuemod.observer.ObserverManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ResidueCommands {

    public static void register(
            CommandDispatcher<ServerCommandSource> dispatcher
    ) {

        dispatcher.register(
                CommandManager.literal("residue")

                        .then(
                                CommandManager.literal("memory")

                                        .executes(context -> {

                                            context.getSource().sendFeedback(
                                                    () -> Text.literal(
                                                            "Memory: "
                                                                    + MemoryManager.getMemory()
                                                                    + " | Attention: "
                                                                    + MemoryManager.getAttention()
                                                    ),
                                                    false
                                            );

                                            return 1;
                                        })
                        )
                        .then(
                                CommandManager.literal("observers")
                                        .executes(context -> {

                                            ObserverManager.getAll()
                                                    .forEach(observer ->

                                                            context.getSource().sendFeedback(
                                                                    () -> Text.literal(
                                                                            observer.getName()
                                                                                    + " | weight="
                                                                                    + observer.getWeight()
                                                                                    + " | used="
                                                                                    + observer.isUsed()
                                                                    ),
                                                                    false
                                                            )
                                                    );

                                            return 1;
                                        })
                        )
        );
    }
}
