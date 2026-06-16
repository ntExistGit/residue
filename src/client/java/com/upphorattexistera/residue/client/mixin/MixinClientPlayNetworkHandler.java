package com.upphorattexistera.residue.client.mixin;

import com.upphorattexistera.residue.client.ResidueClientState;
import com.upphorattexistera.residue.network.ObserverMessageRequestPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {

    @Inject(at = @At("HEAD"), method = "sendChatMessage")
    private void onSendChatMessage(String content, CallbackInfo ci) {

        List<String> mentioned = findMentionedObservers(content);
        if (mentioned.isEmpty()) return;

        String playerName = getPlayerName();
        String contextMessage = playerName != null
                ? playerName + ": " + content
                : content;

        Collections.shuffle(mentioned);
        for (int i = 0; i < mentioned.size(); i++) {
            String observerName = mentioned.get(i);
            long delayMs = i * 1000L;

            Thread.ofVirtual().name("residue-chat-trigger-" + observerName).start(() -> {
                if (delayMs > 0) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ignored) {}
                }

                ClientPlayNetworking.send(
                        new ObserverMessageRequestPacket.Payload(
                                observerName, contextMessage));
            });
        }
    }

    private List<String> findMentionedObservers(String message) {
        List<String> result = new ArrayList<>();
        String lower = message.toLowerCase();

        for (var entry : ResidueClientState.getObservers()) {
            if (lower.contains(entry.name().toLowerCase())) {
                result.add(entry.name());
            }
        }

        return result;
    }

    private String getPlayerName() {
        var client = MinecraftClient.getInstance();
        return client.player != null
                ? client.player.getName().getString()
                : null;
    }
}