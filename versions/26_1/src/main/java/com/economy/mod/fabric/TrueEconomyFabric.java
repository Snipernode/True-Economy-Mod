package com.economy.mod.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import com.economy.mod.fabric.Commands;
import net.minecraft.server.level.ServerPlayer;

public class TrueEconomyFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        TrueEconomyMod.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            wire(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(ServerHooks::onEndTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerHooks.onServerStopped());
    }

    private static void wire(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(net.minecraft.commands.Commands.literal("economy")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                Commands.run(player, new String[0]);
                return 1;
            })
            .then(net.minecraft.commands.Commands.argument("args", StringArgumentType.greedyString())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    String all = ctx.getArgument("args", String.class);
                    Commands.run(player, all.split(" "));
                    return 1;
                })));
    }
}
