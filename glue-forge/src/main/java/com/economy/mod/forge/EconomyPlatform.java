package com.economy.mod.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Per-version bridge between the shared forge glue and MC versions whose
 * mappings shifted (World/Level accessors, GameProfile name, block registry).
 */
public interface EconomyPlatform {
    String playerName(ServerPlayer player);

    Level level(ServerPlayer player);

    String blockName(Block block);

    void send(ServerPlayer player, String msg);
}