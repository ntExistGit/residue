package com.upphorattexistera.residue.client.ai;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Централизованная точка ручного запуска/остановки LLM-сервера.
 * Используется и кнопкой в конфиг-экране, и чат-командами /residueai.
 */
public class LLMServerController {

    private static final AtomicBoolean starting = new AtomicBoolean(false);

    public static boolean isStarting() {
        return starting.get();
    }

    public static void toggle(Consumer<Boolean> onStateChanged) {
        if (LLMServerManager.getInstance().isRunning()) {
            stop(onStateChanged);
        } else {
            start(onStateChanged);
        }
    }

    public static void start(Consumer<Boolean> onStateChanged) {
        LLMServerManager manager = LLMServerManager.getInstance();
        MinecraftClient client = MinecraftClient.getInstance();

        if (manager.isRunning()) {
            notify(client, "§e[Residue] AI server is already running.");
            if (onStateChanged != null) onStateChanged.accept(true);
            return;
        }

        if (!starting.compareAndSet(false, true)) {
            notify(client, "§e[Residue] AI server is already starting.");
            return;
        }

        notify(client, "§e[Residue] Starting AI server...");

        Thread.ofVirtual().name("residue-llm-manual-start").start(() -> {
            boolean success = false;
            String errorMessage = null;
            try {
                manager.startServer();
                success = true;
            } catch (Exception e) {
                errorMessage = e.getMessage();
            } finally {
                starting.set(false);
            }

            boolean finalSuccess = success;
            String finalError = errorMessage;
            client.execute(() -> {
                if (finalSuccess) {
                    notify(client, "§a[Residue] AI server started successfully!");
                } else {
                    notify(client, "§c[Residue] Failed to start AI server: " + finalError);
                }
                if (onStateChanged != null) onStateChanged.accept(manager.isRunning());
            });
        });
    }

    public static void stop(Consumer<Boolean> onStateChanged) {
        LLMServerManager manager = LLMServerManager.getInstance();
        MinecraftClient client = MinecraftClient.getInstance();

        if (!manager.isRunning()) {
            notify(client, "§e[Residue] AI server is not running.");
            if (onStateChanged != null) onStateChanged.accept(false);
            return;
        }

        manager.stopServer();
        notify(client, "§a[Residue] AI server stopped.");
        if (onStateChanged != null) onStateChanged.accept(false);
    }

    private static void notify(MinecraftClient client, String message) {
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal(message));
            }
        });
    }
}