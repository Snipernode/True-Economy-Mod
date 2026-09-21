package com.economy.mod.fabric.adapters;

import net.minecraft.block.Block;
import net.minecraft.util.registry.Registry;

public final class Ids {
    private Ids() {}

    public static String blockName(Block block) {
        return Registry.BLOCK.getId(block).getPath();
    }
}
