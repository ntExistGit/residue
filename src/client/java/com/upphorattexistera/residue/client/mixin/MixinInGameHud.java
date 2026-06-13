package com.upphorattexistera.residue.client.mixin;

import com.upphorattexistera.residue.client.ResidueClientEvents;
import com.upphorattexistera.residue.client.ResidueClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Mixin(InGameHud.class)
public class MixinInGameHud {

    private static final Identifier PING_UNKNOWN = Identifier.ofVanilla("icon/ping_unknown");
    private static final Identifier PING_1 = Identifier.ofVanilla("icon/ping_1");
    private static final Identifier PING_2 = Identifier.ofVanilla("icon/ping_2");
    private static final Identifier PING_3 = Identifier.ofVanilla("icon/ping_3");
    private static final Identifier PING_4 = Identifier.ofVanilla("icon/ping_4");
    private static final Identifier PING_5 = Identifier.ofVanilla("icon/ping_5");

    @Inject(at = @At("HEAD"), method = "renderPlayerList", cancellable = true)
    private void onRenderPlayerList(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {

        if (!ResidueClientEvents.isFakeLanActive()) return;

        MinecraftClient client = MinecraftClient.getInstance();

        if (!client.options.playerListKey.isPressed()) {
            ci.cancel();
            return;
        }

        if (client.getNetworkHandler() == null) {
            ci.cancel();
            return;
        }

        Collection<PlayerListEntry> rawEntries = client.getNetworkHandler().getPlayerList();

        List<PlayerListEntry> entries = new ArrayList<>(rawEntries);
        entries.sort(Comparator.comparingInt(e ->
                ResidueClientState.isObserver(e.getProfile().id()) ? 1 : 0
        ));

        int screenWidth = context.getScaledWindowWidth();

        int maxNameWidth = 0;
        for (PlayerListEntry entry : entries) {
            maxNameWidth = Math.max(maxNameWidth,
                    client.textRenderer.getWidth(entry.getProfile().name()));
        }

        int slotWidth = 9 + 1 + maxNameWidth + 13;
        int slotHeight = 9;
        int padding = 1;

        int startX = screenWidth / 2 - slotWidth / 2;
        int startY = 10;

        int background = client.options.getTextBackgroundColor(553648127);

        context.fill(
                startX - padding,
                startY - padding,
                startX + slotWidth + padding,
                startY + entries.size() * slotHeight + padding,
                Integer.MIN_VALUE
        );

        int y = startY;

        for (PlayerListEntry entry : entries) {

            boolean isObserver = ResidueClientState.isObserver(entry.getProfile().id());

            context.fill(startX, y, startX + slotWidth, y + slotHeight, background);

            // Голова скина
            if (isObserver) {
                Identifier skinId = ResidueClientState.getObserverSkinTexture(
                        entry.getProfile().id());
                if (skinId != null) {
                    // Наш зарегистрированный скин обсервера
                    PlayerSkinDrawer.draw(context, skinId, startX, y, 8, true, false, -1);
                } else {
                    // Скин ещё загружается — рисуем Steve
                    PlayerSkinDrawer.draw(
                            context,
                            Identifier.ofVanilla("textures/entity/player/wide/steve.png"),
                            startX, y, 8, true, false, -1
                    );
                }
            } else {
                SkinTextures skin = entry.getSkinTextures();
                PlayerSkinDrawer.draw(
                        context,
                        skin.body().texturePath(),
                        startX, y, 8,
                        entry.shouldShowHat(), false, -1
                );
            }

            // Имя
            context.drawTextWithShadow(
                    client.textRenderer,
                    entry.getProfile().name(),
                    startX + 9 + 1,
                    y,
                    -1
            );

            // Пинг
            int latency = isObserver
                    ? ResidueClientState.getObserverLatency(entry.getProfile().id())
                    : entry.getLatency();

            renderPingIcon(context, startX, slotWidth, y, latency);

            y += slotHeight;
        }

        ci.cancel();
    }

    private void renderPingIcon(DrawContext context, int x, int slotWidth, int y, int latency) {
        Identifier sprite;
        if (latency < 0) {
            sprite = PING_UNKNOWN;
        } else if (latency < 150) {
            sprite = PING_5;
        } else if (latency < 300) {
            sprite = PING_4;
        } else if (latency < 600) {
            sprite = PING_3;
        } else if (latency < 1000) {
            sprite = PING_2;
        } else {
            sprite = PING_1;
        }

        context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED,
                sprite,
                x + slotWidth - 11,
                y,
                10,
                8
        );
    }
}