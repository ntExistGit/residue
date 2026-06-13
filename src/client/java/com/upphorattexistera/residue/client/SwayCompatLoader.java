package com.upphorattexistera.residue.client;

import com.github.razorplay01.sway.api.SwayAPI;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class SwayCompatLoader {

    public static void register() {
        if (!FabricLoader.getInstance().isModLoaded("sway")) return;

        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer("residue");

        if (modContainer.isPresent()) {
            Optional<Path> pathOpt = modContainer.get().findPath("assets/residue/sway_compat.json");

            if (pathOpt.isPresent()) {
                Path path = pathOpt.get();
                try (InputStream stream = Files.newInputStream(path);
                     Reader reader = new InputStreamReader(stream)) {

                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                    if (json.has("blocks")) {
                        for (JsonElement element : json.getAsJsonArray("blocks")) {
                            JsonObject obj = element.getAsJsonObject();
                            String id = obj.get("id").getAsString();
                            float multiplier = obj.get("multiplier").getAsFloat();

                            Block block = Registries.BLOCK.get(Identifier.of(id));

                            if (block != Blocks.AIR) {
                                SwayAPI.register(block, multiplier);
                                // System.out.println("[Residue] Sway integration: registered " + id);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[Residue] Failed to load sway_compat.json!");
                    e.printStackTrace();
                }
            } else {
                System.err.println("[Residue] Could not find sway_compat.json in resources!");
            }
        }
    }
}