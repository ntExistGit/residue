package com.upphorattexistera.residue.client.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.upphorattexistera.residue.Residue;
import com.upphorattexistera.residue.client.InlineSkinTextureDecoder;
import com.upphorattexistera.residue.client.ResidueClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Перехватывает запрос скина на уровне источника данных (PlayerSkinProvider),
 * а не конечного потребителя (PlayerListEntry / рендер чата) — это даёт
 * совместимость с любым модом, который сам тянет скин через
 * fetchSkinTextures (например, ChatHead), без необходимости патчить
 * каждого потребителя отдельно.
 *
 * Два пути:
 *  1. Скин уже зарегистрирован и закэширован в ResidueClientState
 *     (обычный путь — пришёл через ObserverListPacket) → отдаём мгновенно.
 *  2. Скин пока не закэширован, но он есть прямо в свойствах профиля
 *     (например, профиль был собран сторонним кодом раньше, чем отработал
 *     пакет) → декодируем один раз через общий InlineSkinTextureDecoder
 *     и СРАЗУ кэшируем результат в ResidueClientState, чтобы повторные
 *     вызовы (а ChatHead может звать этот метод часто) попадали в ветку 1.
 */
@Mixin(PlayerSkinProvider.class)
public class MixinPlayerSkinProvider {

    @Inject(
            method = "fetchSkinTextures(Lcom/mojang/authlib/GameProfile;)Ljava/util/concurrent/CompletableFuture;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onFetchSkinTextures(GameProfile profile, CallbackInfoReturnable<CompletableFuture<Optional<SkinTextures>>> cir) {

        // --- Ветка 1: уже закэшировано ---
        Identifier cachedSkin = ResidueClientState.getObserverSkinTexture(profile.id());
        if (cachedSkin != null) {
            boolean isSlim = ResidueClientState.isObserverSlim(profile.id());
            cir.setReturnValue(CompletableFuture.completedFuture(
                    Optional.of(residue$createTextures(cachedSkin, isSlim))));
            return;
        }

        // --- Ветка 2: fallback через свойства профиля ---
        Collection<Property> texturesProps = profile.properties().get("textures");
        if (texturesProps == null || texturesProps.isEmpty()) return;

        Property property = texturesProps.iterator().next();

        Optional<InlineSkinTextureDecoder.DecodedSkin> decoded =
                InlineSkinTextureDecoder.decode(property.value(), profile.name());

        if (decoded.isEmpty()) return;

        try {
            byte[] imageBytes = decoded.get().imageBytes();
            boolean isSlim = decoded.get().isSlim();

            NativeImage image = NativeImage.read(new ByteArrayInputStream(imageBytes));
            NativeImageBackedTexture texture =
                    new NativeImageBackedTexture(() -> "residue_observer_" + profile.name(), image);
            Identifier id = Identifier.of("residue", "observer_skin/" + profile.name().toLowerCase());

            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);

            // Кэшируем сразу, чтобы повторные вызовы fetchSkinTextures для
            // того же профиля попадали в быструю ветку 1, а не декодировали
            // PNG заново при каждом обращении (например, от ChatHead).
            ResidueClientState.registerDecodedSkin(profile.id(), id, isSlim);

            cir.setReturnValue(CompletableFuture.completedFuture(
                    Optional.of(residue$createTextures(id, isSlim))));

        } catch (Exception e) {
            Residue.LOGGER.debug("[Residue] Failed to register fallback skin texture for {}: {}",
                    profile.name(), e.getMessage());
        }
    }

    /**
     * Унифицированная сборка SkinTextures — должна совпадать по структуре
     * с MixinPlayerListEntry и ObserverEntityRenderer, иначе голова обсервера
     * может визуально отличаться между tab-листом, сущностью в мире и чатом.
     */
    @Unique
    private static SkinTextures residue$createTextures(Identifier id, boolean isSlim) {
        AssetInfo.TextureAssetInfo body = new AssetInfo.TextureAssetInfo(id, id);
        return new SkinTextures(
                body, null, null,
                isSlim ? PlayerSkinType.SLIM : PlayerSkinType.WIDE,
                false
        );
    }
}