package com.upphorattexistera.residue.entity;

import com.upphorattexistera.residue.config.ResidueConfig;
import com.upphorattexistera.residue.memory.MemoryStage;
import com.upphorattexistera.residue.observer.ObserverEntitySpawner;
import com.upphorattexistera.residue.observer.ObserverRaycastIgnoreResolver;
import com.upphorattexistera.residue.observer.ObserverStageConfig;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

public class ObserverEntity extends PathAwareEntity {

    private static final TrackedData<String> OBSERVER_NAME =
            DataTracker.registerData(ObserverEntity.class, TrackedDataHandlerRegistry.STRING);

    private static final TrackedData<String> OBSERVER_UUID =
            DataTracker.registerData(ObserverEntity.class, TrackedDataHandlerRegistry.STRING);

    /** Проверяем дистанцию/видимость не каждый тик, а раз в N тиков — дешевле. */
    private static final int DISTANCE_CHECK_INTERVAL = 5;

    /**
     * Половина угла обзора игрока в градусах. Обсервер считается "в поле зрения",
     * если угол между направлением взгляда игрока и направлением на обсервера
     * меньше этого значения.
     *
     * ЕДИНСТВЕННОЕ МЕСТО для настройки угла (без конфига, по требованию).
     * Ориентиры для ручной подстройки:
     *   60   — узкий "тоннельный" обзор, почти прямо перед собой
     *   90   — фокусированное зрение
     *   120  — широкое периферийное зрение (среднее для человека)
     *   140  — очень широкое периферийное зрение
     *   180  — вся фронтальная полусфера
     */
    private static final double FOV_HALF_ANGLE_DEGREES = ResidueConfig.INSTANCE.observerRaycastAngleDegrees;

    /** true, когда обсервер находится в "watch"-зоне и замер, наблюдая за игроком. */
    private boolean isWatching = false;

    public ObserverEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setInvulnerable(true);

        // Звуки шагов и т.п. должны быть слышны — игрок должен чувствовать
        // присутствие рядом, даже когда не видит обсервера.
        this.setSilent(false);

        this.getNavigation().setCanOpenDoors(true);
        this.getNavigation().setCanSwim(true);
        this.setCanPickUpLoot(true);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(OBSERVER_NAME, "");
        builder.add(OBSERVER_UUID, "");
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.MAX_HEALTH, 20.0)
                .add(EntityAttributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 32.0F));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 1.0F, 1.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getEntityWorld().isClient()) return;

        handleTorchAndLantern();
        handleDistanceAndVisibility();
    }

    // ------------------------------------------------------------------
    // Факел / фонарь в офф-руку ночью (поведение не изменилось)
    // ------------------------------------------------------------------

    private void handleTorchAndLantern() {
        boolean isNight = this.getEntityWorld().isNight();
        ItemStack offhandItem = this.getStackInHand(Hand.OFF_HAND);

        if (isNight && offhandItem.isEmpty()) {
            Item lightItem = this.random.nextBoolean() ? Items.TORCH : Items.LANTERN;
            this.setStackInHand(Hand.OFF_HAND, new ItemStack(lightItem));
        } else if (!isNight && (offhandItem.getItem() == Items.TORCH
                || offhandItem.getItem() == Items.LANTERN)) {
            this.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    // ------------------------------------------------------------------
    // Дистанция / видимость / телепортация (аналог эндермена)
    //
    // far (> watchDistance)        -> обсервер живёт своей жизнью (обычные Goal'ы)
    // watch (criticalDist..watch)  -> замирает, смотрит на игрока, ЕСЛИ игрок его видит
    //                                 (рейкаст без преград). Теряет видимость -> телепорт.
    // critical (<= criticalDist)   -> телепорт немедленно, видимость не важна
    //
    // watchDistance/criticalDistance == 0 для текущего стейджа отключает проверку.
    // ------------------------------------------------------------------

    private void handleDistanceAndVisibility() {
        if (this.getEntityWorld().getTime() % DISTANCE_CHECK_INTERVAL != 0) return;

        ServerPlayerEntity player = findNearestPlayer();
        if (player == null) return;

        ObserverStageConfig stageConfig = ObserverStageConfig.forStage(MemoryStage.getCurrentStage());

        double distance = this.getEntityPos().distanceTo(player.getEntityPos());

        // --- Critical zone ---
        if (stageConfig.criticalEnabled() && distance <= stageConfig.criticalDistance()) {
            teleportAway(player);
            return;
        }

        // --- Watch zone ---
        if (stageConfig.watchEnabled() && distance <= stageConfig.watchDistance()) {
            boolean visible = isWithinPlayerFov(player) && hasLineOfSight(player);

            if (visible) {
                enterWatchState(player);
            } else {
                // Был в watch-состоянии и потерял видимость -> телепорт
                if (isWatching) {
                    teleportAway(player);
                }
                exitWatchState();
            }
            return;
        }

        // --- Far zone ---
        if (isWatching) {
            exitWatchState();
        }
    }

    /**
     * Проверка прямой видимости через рейкаст (line of sight), без преград
     * между глазами игрока и глазами обсервера. Угол обзора камеры НЕ учитывается.
     */
    private boolean isWithinPlayerFov(ServerPlayerEntity player) {
        Vec3d lookDirection = player.getRotationVector().normalize();

        Vec3d toObserver = this.getEntityPos()
                .add(0, this.getStandingEyeHeight(), 0)
                .subtract(player.getEyePos())
                .normalize();

        double dot = Math.max(-1.0, Math.min(1.0, lookDirection.dotProduct(toObserver)));
        double angleDegrees = Math.toDegrees(Math.acos(dot));

        return angleDegrees <= FOV_HALF_ANGLE_DEGREES;
    }

    /**
     * Проверяет line of sight пошаговой трассировкой блоков между глазами игрока
     * и обсервера. Блоки из ObserverRaycastIgnoreResolver (по id или тегу) не блокируют.
     */
    private boolean hasLineOfSight(ServerPlayerEntity player) {
        World world = this.getEntityWorld();

        Vec3d from = player.getEyePos();
        Vec3d to = this.getEntityPos().add(0, this.getStandingEyeHeight(), 0);

        Vec3d direction = to.subtract(from);
        double totalDistance = direction.length();
        if (totalDistance < 0.001) return true;

        Vec3d step = direction.normalize().multiply(0.5);
        int steps = (int) Math.ceil(totalDistance / 0.5);

        Vec3d current = from;
        BlockPos lastChecked = null;

        for (int i = 0; i < steps; i++) {
            current = current.add(step);
            BlockPos pos = BlockPos.ofFloored(current);

            if (pos.equals(lastChecked)) continue;
            lastChecked = pos;

            BlockState state = world.getBlockState(pos);
            if (state.isAir()) continue;
            if (ObserverRaycastIgnoreResolver.shouldIgnore(state)) continue;

            if (!state.getCollisionShape(world, pos).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private void enterWatchState(ServerPlayerEntity player) {
        if (!isWatching) {
            isWatching = true;
            this.getNavigation().stop();
        }
        this.getLookControl().lookAt(player, 30.0F, 30.0F);
    }

    private void exitWatchState() {
        isWatching = false;
        // Навигация восстанавливается сама через обычные Goal'ы на следующем тике.
    }

    private void teleportAway(ServerPlayerEntity player) {
        if (!(this.getEntityWorld() instanceof ServerWorld serverWorld)) return;

        boolean success = ObserverEntitySpawner.teleportObserver(
                serverWorld, this, player.getBlockPos());

        if (success) {
            exitWatchState();
        }
        // Если подходящую точку не нашли — остаёмся на месте, попробуем
        // снова через DISTANCE_CHECK_INTERVAL тиков.
    }

    private ServerPlayerEntity findNearestPlayer() {
        return this.getEntityWorld().getPlayers().stream()
                .filter(p -> p instanceof ServerPlayerEntity)
                .map(p -> (ServerPlayerEntity) p)
                .min((a, b) -> Double.compare(
                        a.getEntityPos().distanceTo(this.getEntityPos()),
                        b.getEntityPos().distanceTo(this.getEntityPos())))
                .orElse(null);
    }

    @Override
    public Vec3d getVehicleAttachmentPos(Entity vehicle) {
        Vec3d standard = super.getVehicleAttachmentPos(vehicle);
        return new Vec3d(standard.x, standard.y + 0.5, standard.z);
    }

    public void setObserverName(String name) {
        this.getDataTracker().set(OBSERVER_NAME, name);
        this.setCustomName(Text.literal(name));
        this.setCustomNameVisible(true);
    }

    public String getObserverName() {
        return this.getDataTracker().get(OBSERVER_NAME);
    }

    public void setObserverUuid(UUID uuid) {
        this.getDataTracker().set(OBSERVER_UUID, uuid != null ? uuid.toString() : "");
    }

    public UUID getObserverUuid() {
        String uuidStr = this.getDataTracker().get(OBSERVER_UUID);
        if (uuidStr == null || uuidStr.isEmpty()) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}