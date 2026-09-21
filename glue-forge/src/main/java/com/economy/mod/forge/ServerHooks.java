package com.economy.mod.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-tick economy upkeep + block-step discovery. */
public final class ServerHooks {
    private static final Map<UUID, BlockPos> lastPos = new HashMap<>();

    private ServerHooks() {}

    public static void onEndTick(MinecraftServer server) {
        TrueEconomyMod.core().tick();

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isRemoved()) continue;
            BlockPos feet = p.blockPosition();
            BlockPos prev = lastPos.putIfAbsent(p.getUUID(), feet);
            if (prev != null && !prev.equals(feet)) {
                lastPos.put(p.getUUID(), feet);
                BlockPos under = feet.below();
                String name = TrueEconomyMod.platform.blockName(
                        TrueEconomyMod.platform.level(p).getBlockState(under).getBlock());
                if (name != null && !name.isEmpty() && !"air".equals(name)) {
                    TrueEconomyMod.core().catalog().record("uuid:" + p.getUUID(), name);
                }
            }
        }
    }

    public static void onServerStopped() {
        if (TrueEconomyMod.core() != null) TrueEconomyMod.core().shutdown();
    }
}