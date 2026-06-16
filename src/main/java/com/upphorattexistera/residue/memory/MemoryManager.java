package com.upphorattexistera.residue.memory;

import com.upphorattexistera.residue.Residue;
import com.upphorattexistera.residue.WorldState;
import com.upphorattexistera.residue.config.ResidueConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoryManager {

    private static final String SECRET_PASSWORD = "Yn4Q1hByDkiAsWzF8K91hA-rz6n-2sjkGkaCP8Htw75w";

    private static final AtomicInteger globalMemory = new AtomicInteger(0);

    public static void tick(MinecraftServer server) {

        int max = ResidueConfig.INSTANCE.maxMemory;
        int intervalTicks = ResidueConfig.INSTANCE.memoryIncreaseSeconds * TICKS_PER_SECOND;

        if (intervalTicks <= 0) return;

        if (WorldState.ticks % intervalTicks != 0) return;

        int online = server.getPlayerManager().getCurrentPlayerCount();
        int delta = 1 + (online == 1 ? 1 : online > 1 ? 2 : 0);

        WorldState.memory = globalMemory.updateAndGet(current -> Math.min(current + delta, max));
    }

    private static final int TICKS_PER_SECOND = 20;

    public static int getMemory() {
        return globalMemory.get();
    }

    public static int getAttention() {
        return 0;
    }

    public static void addMemory(int amount) {

        int max = ResidueConfig.INSTANCE.maxMemory;

        WorldState.memory = globalMemory.updateAndGet(current -> {
            int next = current + amount;
            if (next < 0) return 0;
            return Math.min(next, max);
        });
    }

    public static void reset() {
        globalMemory.set(0);
        WorldState.memory = 0;
    }

    private static Path savePath;

    public static void onServerStarted(MinecraftServer server) {
        savePath = server.getSavePath(WorldSavePath.ROOT).resolve("residue/memory.dat");
        load();
    }

    public static void onServerStopping() {
        save();
    }

    private static void save() {
        try {
            if (savePath == null) return;

            byte[] data = intToBytes(globalMemory.get());
            SecretKeySpec key = createKey();

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

            byte[] encrypted = cipher.doFinal(data);

            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

            Files.write(savePath, result);
            Residue.LOGGER.info("[Residue] Memory saved (encrypted)");
        } catch (Exception e) {
            Residue.LOGGER.warn("[Residue] Failed to save memory: {}", e.getMessage());
        }
    }

    private static void load() {
        try {
            if (!Files.exists(savePath)) return;

            byte[] fileData = Files.readAllBytes(savePath);
            if (fileData.length < 17) return;

            byte[] iv = Arrays.copyOfRange(fileData, 0, 16);
            byte[] encrypted = Arrays.copyOfRange(fileData, 16, fileData.length);

            SecretKeySpec key = createKey();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

            byte[] decrypted = cipher.doFinal(encrypted);
            int saved = bytesToInt(decrypted);

            int max = ResidueConfig.INSTANCE.maxMemory;
            globalMemory.set(Math.min(saved, max));
            WorldState.memory = globalMemory.get();
            Residue.LOGGER.info("[Residue] Memory loaded (decrypted): {}", globalMemory.get());
        } catch (Exception e) {
            Residue.LOGGER.warn("[Residue] Failed to load memory: {}", e.getMessage());
        }
    }

    private static SecretKeySpec createKey() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(MemoryManager.SECRET_PASSWORD.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }

    private static byte[] intToBytes(int value) {
        return new byte[] {
                (byte)(value >> 24), (byte)(value >> 16),
                (byte)(value >> 8),  (byte)value
        };
    }

    private static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8)  |  (bytes[3] & 0xFF);
    }
}