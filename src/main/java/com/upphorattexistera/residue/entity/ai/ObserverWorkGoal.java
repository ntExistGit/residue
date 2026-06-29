package com.upphorattexistera.residue.entity.ai;

import com.upphorattexistera.residue.Residue;
import com.upphorattexistera.residue.config.ResidueConfig;
import com.upphorattexistera.residue.entity.ObserverEntity;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Заставляет обсервера, когда он не следит за игроком, искать рядом
 * подходящий блок (дерево/земля-песок-гравий/камень-руда) и "работать"
 * над ним — идти, бить, через заданное время уничтожать блок без дропа,
 * и так до лимита блоков за сессию.
 *
 * ВАЖНО ДЛЯ MODERN YARN: имена ниже (swingHand, startMovingTo, isIdle,
 * breakBlock(pos, drop)) проверены на актуальность под текущие маппинги
 * на момент написания. Если после обновления yarn_mappings что-то из
 * этого не скомпилируется — см. таблицу маппинг-пиков в заметках проекта,
 * это первые кандидаты на переименование.
 */
public class ObserverWorkGoal extends Goal {

    private boolean isFallingTreeLoaded() {
        return FabricLoader.getInstance().isModLoaded("fallingtree");
    }

    private static final Random RANDOM = new Random();
    private static final int TARGET_SEARCH_ATTEMPTS = 40;
    private static final int SWING_INTERVAL_TICKS = 7;

    private final ObserverEntity observer;

    private ObserverWorkType currentType;
    private BlockPos currentTarget;
    private int workTicks;
    private int blocksBrokenThisSession;
    private int swingTimer;

    /** Кулдаун между попытками НАЧАТЬ новую сессию работы (не между ударами по блоку). */
    private int cooldownTicks;

    public ObserverWorkGoal(ObserverEntity observer) {
        this.observer = observer;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (!ResidueConfig.INSTANCE.enableObserverWork) return false;
        if (observer.isWatching() || observer.isPlayerNearby()) return false;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        // Проверяется каждый тик, поэтому делим шанс/сек на 20, чтобы
        // итоговая частота попыток соответствовала настройке "в секунду".
        if (RANDOM.nextDouble() >= ResidueConfig.INSTANCE.observerWorkChancePerSecond / 20.0) {
            return false;
        }

        return findTarget();
    }

    @Override
    public boolean shouldContinue() {
        if (!ResidueConfig.INSTANCE.enableObserverWork) return false;
        if (observer.isWatching() || observer.isPlayerNearby()) return false;
        if (currentTarget == null || currentType == null) return false;

        World world = observer.getEntityWorld();
        return currentType.matches(world.getBlockState(currentTarget));
    }

    @Override
    public void start() {
        observer.setStackInHand(Hand.MAIN_HAND, new ItemStack(currentType.tool));
        workTicks = 0;
        swingTimer = 0;
        blocksBrokenThisSession = 0;
        moveToTarget();
    }

    @Override
    public void stop() {
        currentTarget = null;
        currentType = null;
        observer.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        observer.getNavigation().stop();
        cooldownTicks = 200 + RANDOM.nextInt(400); // 10–30 секунд до следующей попытки
    }

    @Override
    public void tick() {
        if (currentTarget == null) return;

        double distSq = observer.getEntityPos().squaredDistanceTo(
                currentTarget.getX() + 0.5, currentTarget.getY() + 0.5, currentTarget.getZ() + 0.5);

        if (distSq > 4.0) {
            if (observer.getNavigation().isIdle()) {
                moveToTarget();
            }
            return;
        }

        observer.getNavigation().stop();
        observer.getLookControl().lookAt(
                currentTarget.getX() + 0.5,
                currentTarget.getY() + 0.5,
                currentTarget.getZ() + 0.5);

        swingTimer++;
        if (swingTimer >= SWING_INTERVAL_TICKS) {
            swingTimer = 0;
            observer.swingHand(Hand.MAIN_HAND);
        }

        workTicks++;
        if (workTicks >= ResidueConfig.INSTANCE.observerWorkDurationTicks) {
            breakCurrentTarget();
        }
    }

    // ----------------------------------------------------------------

    private void moveToTarget() {
        observer.getNavigation().startMovingTo(
                currentTarget.getX() + 0.5,
                currentTarget.getY(),
                currentTarget.getZ() + 0.5,
                1.0);
    }

    private void breakCurrentTarget() {
        if (observer.getEntityWorld() instanceof ServerWorld serverWorld) {
            boolean drop = ResidueConfig.INSTANCE.observerWorkDropItems;
            BlockState state = serverWorld.getBlockState(currentTarget);

            boolean handledByFallingTree = false;

            if (currentType == ObserverWorkType.AXE && isFallingTreeLoaded()) {
                handledByFallingTree = breakWithFallingTree(serverWorld, currentTarget, state, drop);
            }

            if (!handledByFallingTree) {
                serverWorld.breakBlock(currentTarget, drop);
            }
        }

        blocksBrokenThisSession++;
        workTicks = 0;

        if (blocksBrokenThisSession >= ResidueConfig.INSTANCE.observerWorkMaxBlocksPerSession
                || !findNextTargetNear(currentTarget)) {
            currentTarget = null;
        } else {
            moveToTarget();
        }
    }

    private boolean breakWithFallingTree(ServerWorld world, BlockPos pos, BlockState state, boolean drop) {
        FakePlayer fakePlayer = null;
        try {
            fakePlayer = FakePlayer.get(world);

            fakePlayer.refreshPositionAndAngles(
                    observer.getX(), observer.getY(), observer.getZ(),
                    observer.getYaw(), observer.getPitch());

            ItemStack tool = observer.getMainHandStack().copy();
            fakePlayer.setStackInHand(Hand.MAIN_HAND, tool);

            BlockEntity blockEntity = world.getBlockEntity(pos);

            boolean allow = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(
                    world, fakePlayer, pos, state, blockEntity
            );

            if (!allow) return false;

            world.breakBlock(pos, drop, fakePlayer);

            PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(
                    world, fakePlayer, pos, state, blockEntity
            );

            ItemStack processedTool = fakePlayer.getMainHandStack();
            if (!processedTool.isEmpty() && !observer.getMainHandStack().isEmpty()) {
                observer.getMainHandStack().setDamage(processedTool.getDamage());
            } else if (processedTool.isEmpty()) {
                observer.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            }

            return true;

        } catch (Exception e) {
            Residue.LOGGER.warn("[Residue] FallingTree integration failed for {}: {}",
                    observer.getObserverName(), e.getMessage());
            return false;
        } finally {
            if (fakePlayer != null) {
                fakePlayer.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            }
        }
    }

    /**
     * Ищет случайную подходящую цель в радиусе вокруг обсервера.
     * Случайные смещения вместо полного скана куба — дешевле, и для
     * атмосферной механики не нужна гарантированно ближайшая цель.
     */
    private boolean findTarget() {
        World world = observer.getEntityWorld();
        BlockPos origin = observer.getBlockPos();
        int radius = ResidueConfig.INSTANCE.observerWorkRadius;

        List<ObserverWorkType> shuffled = new ArrayList<>(List.of(ObserverWorkType.values()));
        Collections.shuffle(shuffled, RANDOM);

        for (ObserverWorkType type : shuffled) {
            for (int attempt = 0; attempt < TARGET_SEARCH_ATTEMPTS; attempt++) {
                int dx = RANDOM.nextInt(radius * 2 + 1) - radius;
                int dy = RANDOM.nextInt(7) - 3;
                int dz = RANDOM.nextInt(radius * 2 + 1) - radius;

                BlockPos candidate = origin.add(dx, dy, dz);
                if (type.matches(world.getBlockState(candidate))) {
                    currentType = type;
                    currentTarget = candidate;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean findNextTargetNear(BlockPos lastTarget) {
        World world = observer.getEntityWorld();
        for (int attempt = 0; attempt < TARGET_SEARCH_ATTEMPTS; attempt++) {
            int dx = RANDOM.nextInt(5) - 2;
            int dy = RANDOM.nextInt(3) - 1;
            int dz = RANDOM.nextInt(5) - 2;

            BlockPos candidate = lastTarget.add(dx, dy, dz);
            if (currentType.matches(world.getBlockState(candidate))) {
                currentTarget = candidate;
                return true;
            }
        }
        return false;
    }
}