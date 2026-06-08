package com.upphorattexistera.residuemod.memory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MemoryManager {

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private static final Path FILE =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("residue.json");

    private static ResidueData data;

    public static void load() {

        try {

            if (Files.exists(FILE)) {

                data = GSON.fromJson(
                        Files.readString(FILE),
                        ResidueData.class
                );

            } else {

                data = new ResidueData();
                save();
            }

        } catch (Exception e) {

            e.printStackTrace();
            data = new ResidueData();
        }
    }

    public static void save() {

        try {

            Files.writeString(
                    FILE,
                    GSON.toJson(data)
            );

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public static int getMemory() {
        return data.memory;
    }

    public static int getAttention() {
        return data.attention;
    }

    public static void addMemory(int amount) {

        data.memory += amount;
        save();
    }

    public static void addAttention(int amount) {

        data.attention += amount;
        save();
    }

    public static void setMemory(int value) {

        data.memory = value;
        save();
    }

    public static void setAttention(int value) {

        data.attention = value;
        save();
    }
}