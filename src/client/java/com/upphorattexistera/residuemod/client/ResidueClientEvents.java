package com.upphorattexistera.residuemod.client;

import com.upphorattexistera.residuemod.network.FakeLanPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ResidueClientEvents {

    private static boolean fakeLanActive = false;

    public static boolean isFakeLanActive() {
        return fakeLanActive;
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                FakeLanPacket.ID,
                (payload, context) -> {
                    context.client().execute(() -> fakeLanActive = true);
                }
        );
    }

    public static void reset() {
        fakeLanActive = false;
    }
}