package com.upphorattexistera.residue.client;

import com.upphorattexistera.residue.network.FakeLanPacket;
import com.upphorattexistera.residue.network.ObserverListPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public class ResidueClientEvents {

    private static boolean fakeLanActive = false;

    public static boolean isFakeLanActive() {
        return fakeLanActive;
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                FakeLanPacket.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        fakeLanActive = true;

                        new Thread(() -> {
                            try { Thread.sleep(3000); } catch (Exception ignored) {}
                            context.client().execute(() -> debugTabList());
                        }).start();
                    });
                }
        );
        ClientPlayNetworking.registerGlobalReceiver(
                ObserverListPacket.ID,
                (payload, context) -> {
                    context.client().execute(() ->
                            ResidueClientState.updateObservers(payload.observers())
                    );
                }
        );
    }

    private static void debugTabList() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            System.out.println("[Residue] NetworkHandler is null");
            return;
        }

        client.getNetworkHandler().getPlayerList().forEach(entry ->
                System.out.println("[Residue] Tab entry: " + entry.getProfile().name())
        );
    }

    public static void reset() {
        fakeLanActive = false;
        ResidueClientState.clear();
    }
}