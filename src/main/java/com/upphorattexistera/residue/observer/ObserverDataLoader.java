package com.upphorattexistera.residue.observer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.upphorattexistera.residue.Residue;
import net.minecraft.resource.Resource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;

public class ObserverDataLoader {

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private static final Identifier OBSERVERS_FILE =
            Identifier.of("residue:observers.json");

    public static ObserverDatabase load(MinecraftServer server) {

        try {

            Optional<Resource> resource =
                    server.getResourceManager()
                            .getResource(OBSERVERS_FILE);

            if (resource.isEmpty()) {

                Residue.LOGGER.warn("[Residue] observers.json not found");

                return new ObserverDatabase();
            }

            try (InputStreamReader reader =
                         new InputStreamReader(
                                 resource.get().getInputStream(),
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
                    database.observers = new ArrayList<>();
                }

                return database;
            }

        } catch (Exception e) {

            e.printStackTrace();
            return new ObserverDatabase();
        }
    }
}
