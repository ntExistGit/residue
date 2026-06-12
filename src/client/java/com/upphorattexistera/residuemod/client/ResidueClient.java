package com.upphorattexistera.residuemod.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.upphorattexistera.residuemod.client.ai.ChatAI;
import com.upphorattexistera.residuemod.client.ai.LLMServerManager;
import com.upphorattexistera.residuemod.config.ResidueConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class ResidueClient implements ClientModInitializer {

    private boolean serverStartAttempted = false;

    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("sway")) {
            SwayCompatLoader.register();
        }
        ResidueDebugHud.register();
        ResidueClientEvents.register();

        // 1. Регистрация команды /ai
        registerAiCommand();

        // 2. Событие входа в мир (Автозапуск сервера)
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Сбрасываем флаг при входе в новый мир, чтобы можно было перезапустить если что
            serverStartAttempted = false;

            if (ResidueConfig.INSTANCE.llmEnable && !serverStartAttempted) {
                serverStartAttempted = true;

                // Запускаем в отдельном потоке, чтобы не фризить вход в мир
                new Thread(() -> {
                    try {
                        System.out.println("[Residue] Auto-starting AI Server...");
                        LLMServerManager.getInstance().startServer();

                        // Сообщение об успехе отправляем из главного потока
                        client.execute(() -> {
                            if (client.player != null) {
                                client.player.sendMessage(Text.literal("§a[Residue] AI Server started successfully!"));
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("[Residue] Failed to auto-start AI Server: " + e.getMessage());
                        e.printStackTrace();
                        client.execute(() -> {
                            if (client.player != null) {
                                client.player.sendMessage(Text.literal("§c[Residue] Failed to start AI Server. Check logs."));
                            }
                        });
                    }
                }).start();
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ResidueClientEvents.reset();

            LLMServerManager.getInstance().stopServer();
            serverStartAttempted = false;
        });
    }

    private void registerAiCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("ai")
                    .then(argument("message", StringArgumentType.greedyString())
                            .executes(context -> {
                                String message = StringArgumentType.getString(context, "message");

                                new Thread(() -> {
                                    Text response = ChatAI.ask(message);

                                    context.getSource().getClient().execute(() -> {
                                        context.getSource().sendFeedback(response);
                                    });
                                }).start();
                                return 1;
                            })
                    )
            );
        });
    }
}