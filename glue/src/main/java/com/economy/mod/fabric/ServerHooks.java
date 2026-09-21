package com.economy.mod.fabric;

import com.economy.mod.fabric.adapters.Ids;
import com.economy.mod.fabric.adapters.PlayerAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-tick economy upkeep + block-step discovery. */
public final class ServerHooks {
    private static final Map<UUID, BlockPos> lastPos = new HashMap<>();

    private ServerHooks() {}

    public static void onEndTick(MinecraftServer server) {
        TrueEconomyMod.core().tick();

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (PlayerAccess.removed(p)) continue;
            BlockPos feet = p.getBlockPos();
            BlockPos prev = lastPos.putIfAbsent(p.getUuid(), feet);
            if (prev != null && !prev.equals(feet)) {
                lastPos.put(p.getUuid(), feet);
                boolean onGround = p.isOnGround();
                if (true /* onGround maps differently per version; keep both */) {
                    BlockPos under = feet.down();
                    String name = Ids.blockName(PlayerAccess.world(p).getBlockState(under).getBlock());
                    if (name != null && !name.isEmpty() && !"air".equals(name)) {
                        TrueEconomyMod.core().catalog().record("uuid:" + p.getUuid(), name);
                    }
                }
            }
        }
    }

    public static void onServerStopped() {
        if (TrueEconomyMod.core() != null) TrueEconomyMod.core().shutdown();
    }
}