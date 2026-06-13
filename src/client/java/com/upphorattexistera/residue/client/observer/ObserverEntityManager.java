package com.upphorattexistera.residue.client.observer;

import com.mojang.authlib.GameProfile;
import com.upphorattexistera.residue.client.ResidueClientState;
import com.upphorattexistera.residue.network.ObserverListPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ObserverEntityManager {

    private static final Map<UUID, OtherClientPlayerEntity> activeEntities =
            new ConcurrentHashMap<>();
    private static final Map<UUID, ObserverMovementController> controllers =
            new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    // ----------------------------------------------------------------
    // Вызывается когда список обсерверов обновляется
    // ----------------------------------------------------------------

    public static void sync(java.util.List<ObserverListPacket.ObserverEntry> observers) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        // Удаляем entity которых больше нет
        activeEntities.entrySet().removeIf(entry -> {
            boolean gone = observers.stream()
                    .noneMatch(o -> o.uuid().equals(entry.getKey()));
            if (gone) {
                client.execute(() -> removeEntity(entry.getKey(), client.world));
            }
            return gone;
        });

        // Добавляем новых
        for (ObserverListPacket.ObserverEntry observer : observers) {
            if (!activeEntities.containsKey(observer.uuid())) {
                client.execute(() -> spawnEntity(observer, client.world));
            }
        }
    }

    public static void clear() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            for (UUID uuid : activeEntities.keySet()) {
                removeEntity(uuid, client.world);
            }
        }
        activeEntities.clear();
        controllers.clear();
    }

    // ----------------------------------------------------------------
    // Тик — вызывать каждый клиентский тик
    // ----------------------------------------------------------------

    // ObserverEntityManager.java — метод tick()
    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        for (Map.Entry<UUID, OtherClientPlayerEntity> entry : activeEntities.entrySet()) {
            UUID uuid = entry.getKey();
            OtherClientPlayerEntity entity = entry.getValue();
            ObserverMovementController controller = controllers.get(uuid);

            if (controller == null) continue;

            Vec3d next = controller.tick(
                    entity.getEntityPos(),
                    client.player.getEntityPos(),
                    client.world
            );

            // Прилипаем к поверхности
            BlockPos blockPos = new BlockPos((int) next.x, (int) next.y, (int) next.z);
            int surfaceY = client.world.getTopY(
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    (int) next.x, (int) next.z);

            double targetY = surfaceY; // стоим на поверхности

            // Плавно интерполируем Y
            double currentY = entity.getEntityPos().y;
            double newY = currentY + (targetY - currentY) * 0.2;

            entity.setPosition(next.x, newY, next.z);
            entity.setYaw(controller.getYaw());
            entity.setHeadYaw(controller.getYaw());
            entity.setPitch(0);

            // Анимация действия
            controller.tickAnimation(entity, client.world);
        }
    }

    // ----------------------------------------------------------------
    // Спавн entity
    // ----------------------------------------------------------------

    private static void spawnEntity(ObserverListPacket.ObserverEntry observer,
                                    ClientWorld world) {
        GameProfile profile = new GameProfile(observer.uuid(), observer.name());
        OtherClientPlayerEntity entity = new OtherClientPlayerEntity(world, profile);

        // Стартовая позиция — случайная точка вокруг игрока
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d playerPos = client.player != null
                ? client.player.getEntityPos()
                : Vec3d.ZERO;

        double angle = RANDOM.nextDouble() * Math.PI * 2;
        double distance = 30 + RANDOM.nextDouble() * 50; // 30-80 блоков
        double startX = playerPos.x + Math.cos(angle) * distance;
        double startZ = playerPos.z + Math.sin(angle) * distance;
        double startY = playerPos.y;

        entity.setPosition(startX, startY, startZ);
        entity.noClip = true;

        // Добавляем в мир
        world.addEntity(entity);
        activeEntities.put(observer.uuid(), entity);

        // Создаём контроллер движения
        controllers.put(observer.uuid(),
                new ObserverMovementController(startX, startY, startZ));

        System.out.println("[Residue] Observer entity spawned: " + observer.name()
                + " at " + (int)startX + " " + (int)startY + " " + (int)startZ);
    }

    private static void removeEntity(UUID uuid, ClientWorld world) {
        OtherClientPlayerEntity entity = activeEntities.remove(uuid);
        controllers.remove(uuid);
        if (entity != null) {
            world.removeEntity(entity.getId(), null);
        }
    }

    public static Map<UUID, OtherClientPlayerEntity> getActiveEntities() {
        return activeEntities;
    }
}