package com.upphorattexistera.residuemod.command.sub;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.upphorattexistera.residuemod.config.ResidueConfig;
import com.upphorattexistera.residuemod.memory.MemoryManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class MemoryCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("memory")
                .executes(MemoryCommand::showMemory)

                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("amount",
                                IntegerArgumentType.integer(0, ResidueConfig.INSTANCE.maxMemory))
                                .executes(MemoryCommand::setMemory)));
    }

    private static int showMemory(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> Text.literal(
                "Memory: " + MemoryManager.getMemory() +
                        " | Attention: " + MemoryManager.getAttention()),
                false);
        return 1;
    }

    private static int setMemory(CommandContext<ServerCommandSource> context) {
        int amount = IntegerArgumentType.getInteger(context, "amount");
        int current = MemoryManager.getMemory();
        MemoryManager.addMemory(amount - current);

        context.getSource().sendFeedback(() -> Text.literal(
                "Memory set to " + MemoryManager.getMemory()),
                false);

        return 1;
    }
}