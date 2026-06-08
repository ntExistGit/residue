package com.upphorattexistera.residuemod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.upphorattexistera.residuemod.memory.MemoryManager;
import com.upphorattexistera.residuemod.observer.ObserverManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ResidueCommands {

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {

        dispatcher.register(
                Commands.literal("residue")

                        .then(
                                Commands.literal("memory")

                                        .executes(context -> {

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(
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
                                Commands.literal("observers")
                                        .executes(context -> {

                                            ObserverManager.getAll()
                                                    .forEach(observer ->

                                                            context.getSource().sendSuccess(
                                                                    () -> Component.literal(
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