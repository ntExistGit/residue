package com.upphorattexistera.residue.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.ClientPlayerLikeEntity;
import net.minecraft.client.network.ClientPlayerLikeState;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class ObserverEntity extends PlayerLikeEntity implements ClientPlayerLikeEntity {

    private UUID observerUuid;
    private String observerName;
    private GameProfile gameProfile;

    // Для ClientPlayerLikeEntity
    private final ClientPlayerLikeState clientState = new ClientPlayerLikeState();
    private SkinTextures cachedSkin = null;

    // AI
    private final MobNavigation navigation;

    public ObserverEntity(EntityType<? extends PlayerLikeEntity> type, World world) {
        super(type, world);
        this.setInvulnerable(true);
        this.setSilent(true);
        this.noClip = false;

        // Навигация — обходит блоки как моб
        this.navigation = new MobNavigation(this, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.MAX_HEALTH, 20.0);
    }

    @Override
    protected void initGoals() {
        // Бродит случайно
        this.goalSelector.add(1, new WanderAroundFarGoal(this, 0.8, 0.002f));
        // Смотрит по сторонам
        this.goalSelector.add(2, new LookAroundGoal(this));
        // Иногда смотрит на игрока
        this.goalSelector.add(3, new LookAtEntityGoal(this,
                net.minecraft.entity.player.PlayerEntity.class, 8.0f, 0.02f));
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        return new MobNavigation(this, world);
    }

    // ----------------------------------------------------------------
    // PlayerLikeEntity — обязательный абстрактный метод
    // ----------------------------------------------------------------

    @Override
    public ProfileComponent getMannequinProfile() {
        if (gameProfile != null) {
            return new ProfileComponent(gameProfile);
        }
        return new ProfileComponent(new GameProfile(
                observerUuid != null ? observerUuid : UUID.randomUUID(),
                observerName != null ? observerName : "Observer"
        ));
    }

    // ----------------------------------------------------------------
    // ClientPlayerLikeEntity
    // ----------------------------------------------------------------

    @Override
    public ClientPlayerLikeState getState() {
        return clientState;
    }

    @Override
    public SkinTextures getSkin() {
        if (cachedSkin != null) return cachedSkin;
        // Возвращаем дефолтный скин пока не загружен
        return net.minecraft.client.util.DefaultSkinHelper.getSkinTextures(
                observerUuid != null ? observerUuid : UUID.randomUUID()
        );
    }

    @Override
    public @Nullable ParrotEntity.Variant getShoulderParrotVariant(boolean leftShoulder) {
        return null;
    }

    @Override
    public boolean hasExtraEars() {
        return false;
    }

    // ----------------------------------------------------------------
    // Все части модели видимы
    // ----------------------------------------------------------------

    @Override
    public boolean isModelPartVisible(PlayerModelPart part) {
        return true;
    }

    // ----------------------------------------------------------------
    // Не колидится с игроком, не показывает имя
    // ----------------------------------------------------------------

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean isCustomNameVisible() {
        return false;
    }

    @Override
    public boolean canBeSetOnFire() {
        return false;
    }

    // ----------------------------------------------------------------
    // Тик — обновляем ClientPlayerLikeState для анимации
    // ----------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        clientState.tick(this.getEntityPos(), this.getVelocity());
    }

    @Override
    protected void addDistanceMoved(float distanceMoved) {
        clientState.addDistanceMoved(distanceMoved);
    }

    // ----------------------------------------------------------------
    // Сеттеры
    // ----------------------------------------------------------------

    public void setObserverUuid(UUID uuid) {
        this.observerUuid = uuid;
    }

    public UUID getObserverUuid() {
        return observerUuid;
    }

    public void setObserverName(String name) {
        this.observerName = name;
    }

    public String getObserverName() {
        return observerName;
    }

    public void setGameProfile(GameProfile profile) {
        this.gameProfile = profile;
    }

    public void setCachedSkin(SkinTextures skin) {
        this.cachedSkin = skin;
    }
}