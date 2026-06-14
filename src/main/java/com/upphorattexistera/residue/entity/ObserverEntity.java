package com.upphorattexistera.residue.entity;

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
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

public class ObserverEntity extends PathAwareEntity {

    private static final TrackedData<String> OBSERVER_NAME =
            DataTracker.registerData(ObserverEntity.class, TrackedDataHandlerRegistry.STRING);

    private static final TrackedData<String> OBSERVER_UUID =
            DataTracker.registerData(ObserverEntity.class, TrackedDataHandlerRegistry.STRING);

    public ObserverEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setInvulnerable(true);
        this.setSilent(true);
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
        if (!this.getEntityWorld().isClient()) {
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
    }

    @Override
    public Vec3d getVehicleAttachmentPos(Entity vehicle) {
        Vec3d standard = super.getVehicleAttachmentPos(vehicle);
        return new Vec3d(standard.x, standard.y + 0.5, standard.z); // подбери значение
    }

    public void setObserverName(String name) {
        this.getDataTracker().set(OBSERVER_NAME, name);
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