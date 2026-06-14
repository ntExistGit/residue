package com.upphorattexistera.residue.client.ai;

import com.upphorattexistera.residue.client.ResidueClientEvents;
import com.upphorattexistera.residue.config.ResidueConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.text.Text;

public class ResidueClientConnectionEvents {

    private static boolean serverStartAttempted = false;

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            serverStartAttempted = false;

            if (ResidueConfig.INSTANCE.llmEnable && !serverStartAttempted) {
                serverStartAttempted = true;

                new Thread(() -> {
                    try {
                        System.out.println("[Residue] Auto-starting AI Server...");
                        LLMServerManager.getInstance().startServer();

                        client.execute(() -> {
                            if (client.player != null) {
                                client.player.sendMessage(Text.literal("§a[Residue] AI Server started successfully!"));
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("[Residue] Failed to auto-start AI Server: " + e.getMessage());
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
}