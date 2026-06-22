package com.upphorattexistera.residue.client.render;

import com.upphorattexistera.residue.client.ResidueClientState;
import com.upphorattexistera.residue.entity.ObserverEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

public class ObserverEntityRenderer
        extends BipedEntityRenderer<ObserverEntity, PlayerEntityRenderState, PlayerEntityModel> {

    private static final Identifier FALLBACK_SKIN =
            Identifier.ofVanilla("textures/entity/player/wide/steve.png");

    private static final EntityModelLayer SLIM_LAYER =
            new EntityModelLayer(Identifier.ofVanilla("player_slim"), "main");

    private final PlayerEntityModel wideModel;
    private final PlayerEntityModel slimModel;

    public ObserverEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx,
                new PlayerEntityModel(ctx.getPart(EntityModelLayers.PLAYER), false),
                new PlayerEntityModel(ctx.getPart(EntityModelLayers.PLAYER), false),
                0.5f);
        this.wideModel = new PlayerEntityModel(
                ctx.getPart(EntityModelLayers.PLAYER), false);
        this.slimModel = new PlayerEntityModel(
                ctx.getPart(SLIM_LAYER), true);
    }

    @Override
    public PlayerEntityRenderState createRenderState() {
        return new PlayerEntityRenderState();
    }

    @Override
    public void updateRenderState(ObserverEntity entity,
                                  PlayerEntityRenderState state,
                                  float partialTicks) {
        super.updateRenderState(entity, state, partialTicks);

        state.hatVisible = true;
        state.jacketVisible = true;
        state.leftSleeveVisible = true;
        state.rightSleeveVisible = true;
        state.leftPantsLegVisible = true;
        state.rightPantsLegVisible = true;
        state.capeVisible = false;
        state.spectator = false;

        // Имя обсервера должно быть видно всегда, независимо от
        // настроек клиента "показывать имена" — это часть атмосферы:
        // игрок должен точно знать, кто на него смотрит.
        //state.nameLabelVisible = true;
        //state.displayName = Text.literal(entity.getObserverName());

        Identifier skinId = FALLBACK_SKIN;
        boolean isSlim = false;

        if (entity.getObserverUuid() != null) {
            Identifier skin = ResidueClientState.getObserverSkinTexture(
                    entity.getObserverUuid());
            if (skin != null) skinId = skin;
            isSlim = ResidueClientState.isObserverSlim(entity.getObserverUuid());
        }

        this.model = isSlim ? slimModel : wideModel;

        AssetInfo.TextureAssetInfo body =
                new AssetInfo.TextureAssetInfo(skinId, skinId);

        state.skinTextures = new SkinTextures(
                body, null, null,
                isSlim ? PlayerSkinType.SLIM : PlayerSkinType.WIDE,
                false
        );
    }

    @Override
    public Identifier getTexture(PlayerEntityRenderState state) {
        return state.skinTextures.body().texturePath();
    }
}