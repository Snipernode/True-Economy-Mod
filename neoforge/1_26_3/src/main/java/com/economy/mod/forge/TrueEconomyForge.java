package com.economy.mod.forge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(TrueEconomyForge.MODID)
public class TrueEconomyForge {
    public static final String MODID = "trueeconomy";

    public TrueEconomyForge(IEventBus modBus) {
        TrueEconomyMod.init(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get(), new Adapter());
        IEventBus gameBus = NeoForge.EVENT_BUS;
        gameBus.addListener(this::onRegisterCommands);
        gameBus.addListener(this::onTick);
        gameBus.addListener(this::onStopping);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        Commands.register(dispatcher);
    }

    private void onTick(ServerTickEvent.Post event) {
        if (event.getServer() != null) {
            ServerHooks.onEndTick(event.getServer());
        }
    }

    private void onStopping(ServerStoppingEvent event) {
        ServerHooks.onServerStopped();
    }
}