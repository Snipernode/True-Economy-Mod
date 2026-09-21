package com.economy.mod.fabric.adapters;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

/** Cross-version accessors for entity state that changed names over versions. */
public final class PlayerAccess {
    private PlayerAccess() {}

    public static boolean removed(ServerPlayerEntity p) {
        return p.removed;
    }

    public static World world(ServerPlayerEntity p) {
        return p.world;
    }

    public static String name(ServerPlayerEntity p) {
        return p.getGameProfile().getName();
    }
}
