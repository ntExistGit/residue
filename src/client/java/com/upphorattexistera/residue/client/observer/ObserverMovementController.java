package com.upphorattexistera.residue.client.observer;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.Random;

public class ObserverMovementController {

    private static final Random RANDOM = new Random();

    private double x;
    private double y;
    private double z;
    private float yaw;

    private double targetX;
    private double targetZ;
    private int ticksToNextTarget = 0;

    // Скорость ходьбы — как у обычного игрока (~4.3 блока/сек = 0.215 блока/тик)
    private static final double SPEED = 0.18;

    private static final double MIN_DIST = 30;
    private static final double MAX_DIST = 70;

    // Анимация
    private int actionCooldown = 0;
    private boolean isMoving = false;

    public ObserverMovementController(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.targetX = x;
        this.targetZ = z;
    }

    public Vec3d tick(Vec3d currentPos, Vec3d playerPos, ClientWorld world) {
        this.x = currentPos.x;
        this.y = currentPos.y;
        this.z = currentPos.z;

        double distToTarget = Math.sqrt(
                Math.pow(x - targetX, 2) + Math.pow(z - targetZ, 2));

        if (distToTarget < 0.5 || ticksToNextTarget <= 0) {
            pickNewTarget(playerPos, world);
            ticksToNextTarget = 100 + RANDOM.nextInt(200);
        }

        ticksToNextTarget--;

        double dx = targetX - x;
        double dz = targetZ - z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist > SPEED) {
            x += (dx / dist) * SPEED;
            z += (dz / dist) * SPEED;
            yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            isMoving = true;
        } else {
            isMoving = false;
        }

        return new Vec3d(x, y, z);
    }

    public void tickAnimation(OtherClientPlayerEntity entity, ClientWorld world) {
        if (actionCooldown > 0) {
            actionCooldown--;
            return;
        }

        BlockPos pos = entity.getBlockPos();
        boolean underground = isUnderground(world, pos);

        if (isMoving) {
            // Во время ходьбы — редкий свинг рукой как будто что-то делает
            if (RANDOM.nextInt(40) == 0) {
                entity.swingHand(Hand.MAIN_HAND);
                actionCooldown = 10;
            }
        } else {
            // Стоит на месте — активно работает
            if (underground) {
                // В пещере — бьёт киркой
                entity.swingHand(Hand.MAIN_HAND);
                actionCooldown = 8 + RANDOM.nextInt(6);
            } else {
                // На поверхности — рубит дерево или просто осматривается
                boolean nearTree = hasTreeNearby(world, pos);
                if (nearTree) {
                    entity.swingHand(Hand.MAIN_HAND);
                    actionCooldown = 10 + RANDOM.nextInt(8);
                } else {
                    // Осматривается — поворачивает голову
                    if (RANDOM.nextInt(60) == 0) {
                        entity.setHeadYaw(entity.getHeadYaw() + RANDOM.nextFloat() * 90 - 45);
                        actionCooldown = 40 + RANDOM.nextInt(40);
                    }
                }
            }
        }
    }

    private boolean isUnderground(ClientWorld world, BlockPos pos) {
        int surfaceY = world.getTopY(
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return pos.getY() < surfaceY - 3;
    }

    private boolean hasTreeNearby(ClientWorld world, BlockPos center) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = 0; dy <= 5; dy++) {
                    BlockPos check = center.add(dx, dy, dz);
                    BlockState state = world.getBlockState(check);
                    if (state.isIn(BlockTags.LOGS)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void pickNewTarget(Vec3d playerPos, ClientWorld world) {
        // Пробуем несколько раз найти точку на поверхности
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double distance = MIN_DIST + RANDOM.nextDouble() * (MAX_DIST - MIN_DIST);

            double candidateX = playerPos.x + Math.cos(angle) * distance;
            double candidateZ = playerPos.z + Math.sin(angle) * distance;

            int surfaceY = world.getTopY(
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    (int) candidateX, (int) candidateZ);

            // Берём только если поверхность разумная (не бездна, не небо)
            if (surfaceY > world.getBottomY() + 5
                    && surfaceY < world.getBottomY() + world.getHeight() - 5) {
                targetX = candidateX;
                targetZ = candidateZ;
                return;
            }
        }

        // Если не нашли — просто рядом с игроком
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        targetX = playerPos.x + Math.cos(angle) * MIN_DIST;
        targetZ = playerPos.z + Math.sin(angle) * MIN_DIST;
    }

    public float getYaw() {
        return yaw;
    }

    public boolean isMoving() {
        return isMoving;
    }
}