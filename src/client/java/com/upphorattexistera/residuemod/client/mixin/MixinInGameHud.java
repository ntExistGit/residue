package com.upphorattexistera.residuemod.client.mixin;

import com.upphorattexistera.residuemod.client.ResidueClientEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinInGameHud {

    @Inject(at = @At("HEAD"), method = "renderPlayerList", cancellable = true)
    private void onRenderPlayerList(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {

        if (!ResidueClientEvents.isFakeLanActive()) return;

        InGameHud self = (InGameHud) (Object) this;
        MinecraftClient client = ((InGameHudAccessor) self).getClient();

        if (!client.options.playerListKey.isPressed()) {
            self.getPlayerListHud().setVisible(false);
            return;
        }

        self.getPlayerListHud().setVisible(true);
        context.createNewRootLayer();
        self.getPlayerListHud().render(
                context,
                context.getScaledWindowWidth(),
                client.world.getScoreboard(),
                null
        );

        ci.cancel();
    }
}