package com.upphorattexistera.residue.client.mixin;

import com.upphorattexistera.residue.client.ResidueClientState;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public class MixinPlayerListEntry {

    @Inject(at = @At("HEAD"), method = "getSkinTextures", cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        PlayerListEntry self = (PlayerListEntry) (Object) this;

        if (!ResidueClientState.isObserver(self.getProfile().id())) return;

        Identifier skinId = ResidueClientState.getObserverSkinTexture(
                self.getProfile().id());

        if (skinId == null) return;

        boolean isSlim = ResidueClientState.isObserverSlim(self.getProfile().id());

        // TextureAssetInfo(id, texturePath) — второй аргумент это путь к файлу
        AssetInfo.TextureAssetInfo body = new AssetInfo.TextureAssetInfo(skinId, skinId);

        SkinTextures skinTextures = new SkinTextures(
                body,
                null,
                null,
                isSlim ? PlayerSkinType.SLIM : PlayerSkinType.WIDE,
                false
        );

        cir.setReturnValue(skinTextures);
    }
}