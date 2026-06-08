package com.upphorattexistera.residuemod.observer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ObserverDataLoader {

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private static final Identifier OBSERVERS_FILE =
            Identifier.parse(
                    "residue:observers.json"
            );

    public static ObserverDatabase load(MinecraftServer server) {

        try {

            Optional<Resource> resource =
                    server.getResourceManager()
                            .getResource(OBSERVERS_FILE);

            if (resource.isEmpty()) {

                System.err.println(
                        "[Residue] observers.json not found"
                );

                return new ObserverDatabase();
            }

            try (InputStreamReader reader =
                         new InputStreamReader(
                                 resource.get().open(),
                                 StandardCharsets.UTF_8
                         )) {

                ObserverDatabase database =
                        GSON.fromJson(
                                reader,
                                ObserverDatabase.class
                        );

                if (database == null) {
                    return new ObserverDatabase();
                }

                if (database.observers == null) {
                    database.observers.clear();
                }

                return database;
            }

        } catch (Exception e) {

            e.printStackTrace();
            return new ObserverDatabase();
        }
    }
}