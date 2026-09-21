package com.economy.mod.forge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class Adapter implements EconomyPlatform {
    @Override
    public String playerName(ServerPlayer player) {
        return player.getGameProfile().getName();
    }

    @Override
    public Level level(ServerPlayer player) {
        return player.level();
    }

    @Override
    public String blockName(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    @Override
    public void send(ServerPlayer player, String msg) {
        player.displayClientMessage(Component.literal(msg), false);
    }
}
