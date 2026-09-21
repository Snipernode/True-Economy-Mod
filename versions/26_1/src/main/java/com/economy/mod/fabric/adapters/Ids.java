package com.economy.mod.fabric.adapters;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

public final class Ids {
    private Ids() {}

    public static String blockName(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }
}
