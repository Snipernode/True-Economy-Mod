package com.economy.mod.forge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(TrueEconomyForge.MODID)
public class TrueEconomyForge {
    public static final String MODID = "trueeconomy";

    public TrueEconomyForge(net.neoforged.bus.api.IEventBus modBus) {
        TrueEconomyMod.init(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get(), new Adapter());
        net.neoforged.bus.api.IEventBus gameBus = NeoForge.EVENT_BUS;
        gameBus.addListener(this::onRegisterCommands);
        gameBus.addListener(this::onTick);
        gameBus.addListener(this::onStopping);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        Commands.register(dispatcher);
    }

    private void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer() != null) {
            ServerHooks.onEndTick(event.getServer());
        }
    }

    private void onStopping(ServerStoppingEvent event) {
        ServerHooks.onServerStopped();
    }
}