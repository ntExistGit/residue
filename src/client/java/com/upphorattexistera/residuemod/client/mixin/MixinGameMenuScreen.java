package com.upphorattexistera.residuemod.client.mixin;

import com.upphorattexistera.residuemod.client.ResidueClientEvents;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public class MixinGameMenuScreen {

    @Inject(at = @At("RETURN"), method = "initWidgets")
    private void onInitWidgets(CallbackInfo ci) {

        if (!ResidueClientEvents.isFakeLanActive()) return;

        GameMenuScreen self = (GameMenuScreen) (Object) this;

        Text lanText = Text.translatable("menu.shareToLan");

        self.children().stream()
                .filter(w -> w instanceof ButtonWidget)
                .map(w -> (ButtonWidget) w)
                .filter(b -> b.getMessage().getString().equals(lanText.getString()))
                .findFirst()
                .ifPresent(b -> b.active = false);
    }
}