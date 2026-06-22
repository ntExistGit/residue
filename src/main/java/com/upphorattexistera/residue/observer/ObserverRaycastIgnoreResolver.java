package com.upphorattexistera.residue.observer;

import com.upphorattexistera.residue.config.ResidueConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public class ObserverRaycastIgnoreResolver {

    private static Set<Identifier> ignoredBlockIds = null;
    private static Set<TagKey<Block>> ignoredBlockTags = null;

    /** Вызвать после изменения конфига в YACL, чтобы перечитать список. */
    public static void invalidate() {
        ignoredBlockIds = null;
        ignoredBlockTags = null;
    }

    public static boolean shouldIgnore(BlockState state) {
        ensureParsed();

        Identifier blockId = Registries.BLOCK.getId(state.getBlock());
        if (ignoredBlockIds.contains(blockId)) return true;

        for (TagKey<Block> tag : ignoredBlockTags) {
            if (state.isIn(tag)) return true;
        }
        return false;
    }

    private static void ensureParsed() {
        if (ignoredBlockIds != null) return;

        ignoredBlockIds = new HashSet<>();
        ignoredBlockTags = new HashSet<>();

        for (String raw : ResidueConfig.INSTANCE.observerRaycastIgnoreBlocks) {
            if (raw == null || raw.isBlank()) continue;
            String trimmed = raw.trim();

            try {
                if (trimmed.startsWith("#")) {
                    Identifier id = Identifier.of(trimmed.substring(1));
                    ignoredBlockTags.add(TagKey.of(RegistryKeys.BLOCK, id));
                } else {
                    ignoredBlockIds.add(Identifier.of(trimmed));
                }
            } catch (Exception e) {
                // Некорректный формат строки — пропускаем эту запись
            }
        }
    }
}