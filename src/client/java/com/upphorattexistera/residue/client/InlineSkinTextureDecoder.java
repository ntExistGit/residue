package com.upphorattexistera.residue.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.upphorattexistera.residue.Residue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Общая логика декодирования "inline" скина (data URI base64 PNG),
 * упакованного в property "textures" GameProfile по соглашению,
 * используемому ObserverSkinResolver.
 *
 * Используется как из ResidueClientState (асинхронный путь через
 * ObserverListPacket), так и из MixinPlayerSkinProvider (синхронный
 * fallback-путь через GameProfile.properties()), чтобы не дублировать
 * парсинг в двух местах.
 */
public class InlineSkinTextureDecoder {

    private static final String DATA_URI_PREFIX = "data:image/png;base64,";

    public record DecodedSkin(byte[] imageBytes, boolean isSlim) {}

    /**
     * @param base64PropertyValue значение property "textures" (base64 JSON,
     *                            как собирается в ObserverSkinResolver.buildSkinData)
     */
    public static Optional<DecodedSkin> decode(String base64PropertyValue, String contextName) {
        try {
            String decoded = new String(
                    Base64.getDecoder().decode(base64PropertyValue), StandardCharsets.UTF_8);

            JsonObject root = JsonParser.parseString(decoded).getAsJsonObject();
            if (!root.has("textures")) return Optional.empty();

            JsonObject textures = root.getAsJsonObject("textures");
            if (textures == null || !textures.has("SKIN")) return Optional.empty();

            JsonObject skinObj = textures.getAsJsonObject("SKIN");
            String skinUrl = skinObj.get("url").getAsString();

            if (!skinUrl.startsWith(DATA_URI_PREFIX)) {
                Residue.LOGGER.debug("[Residue] Unexpected skin URL format for {}", contextName);
                return Optional.empty();
            }

            String imageBase64 = skinUrl.substring(DATA_URI_PREFIX.length());
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);

            boolean isSlim = false;
            if (skinObj.has("metadata")) {
                JsonObject metadata = skinObj.getAsJsonObject("metadata");
                if (metadata != null && metadata.has("model")) {
                    isSlim = "slim".equals(metadata.get("model").getAsString());
                }
            }

            return Optional.of(new DecodedSkin(imageBytes, isSlim));

        } catch (Exception e) {
            Residue.LOGGER.debug("[Residue] Failed to decode inline skin texture for {}: {}",
                    contextName, e.getMessage());
            return Optional.empty();
        }
    }
}