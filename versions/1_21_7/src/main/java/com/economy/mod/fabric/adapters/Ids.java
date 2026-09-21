package com.economy.mod.fabric.adapters;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;

public final class Ids {
    private Ids() {}

    public static String blockName(Block block) {
        return Registries.BLOCK.getId(block).getPath();
    }
}
