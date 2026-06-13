package com.upphorattexistera.residue.command;

import com.upphorattexistera.residue.command.sub.MemoryCommand;
import com.upphorattexistera.residue.command.sub.ObserverCommand;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class ResidueCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var residue = CommandManager.literal("residue");

        residue.then(MemoryCommand.build());
        residue.then(ObserverCommand.build());

        dispatcher.register(residue);
    }
}