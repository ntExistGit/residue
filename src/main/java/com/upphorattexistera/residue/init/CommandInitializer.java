package com.upphorattexistera.residue.init;

import com.upphorattexistera.residue.command.ResidueCommands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class CommandInitializer {
    public static void init() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        ResidueCommands.register(dispatcher)
        );
    }
}