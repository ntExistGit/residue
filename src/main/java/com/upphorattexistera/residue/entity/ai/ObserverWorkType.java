package com.upphorattexistera.residue.entity.ai;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;

/**
 * Виды "работы" обсервера, когда он не следит за игроком. Каждый тип
 * привязан к инструменту в главную руку и предикату блоков-целей.
 */
public enum ObserverWorkType {

    AXE(Items.IRON_AXE) {
        @Override
        public boolean matches(BlockState state) {
            return state.isIn(BlockTags.LOGS);
        }
    },

    SHOVEL(Items.IRON_SHOVEL) {
        @Override
        public boolean matches(BlockState state) {
            return state.isIn(BlockTags.DIRT)
                    || state.isIn(BlockTags.SAND)
                    || state.isIn(ConventionalBlockTags.GRAVELS);
        }
    },

    PICKAXE(Items.IRON_PICKAXE) {
        @Override
        public boolean matches(BlockState state) {
            return state.isIn(BlockTags.BASE_STONE_OVERWORLD)
                    || state.isIn(BlockTags.BASE_STONE_NETHER)
                    || state.isIn(ConventionalBlockTags.ORES)
                    || state.getBlock() == Blocks.ANDESITE
                    || state.getBlock() == Blocks.DIORITE
                    || state.getBlock() == Blocks.GRANITE
                    || state.getBlock() == Blocks.TUFF;
        }
    };

    public final Item tool;

    ObserverWorkType(Item tool) {
        this.tool = tool;
    }

    public abstract boolean matches(BlockState state);
}