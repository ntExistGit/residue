package com.upphorattexistera.residue.command.sub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.upphorattexistera.residue.event.events.FakeLanEvent;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class FakeLanCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("fakelantrigger")
                .executes(ctx -> {
                    FakeLanEvent.forceTrigger(ctx.getSource().getServer());
                    return 1;
                });
    }
}
