package com.upphorattexistera.residue.client.mixin;

import com.upphorattexistera.residue.event.events.FakeLanEvent;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Shadow
    private volatile boolean paused;

    @Inject(method = "frame(Z)V", at = @At("TAIL"))
    private void residue$overridePauseForFakeLan(boolean tick, CallbackInfo ci) {
        if (FakeLanEvent.isActive()) {
            this.paused = false;
        }
    }
}