package com.economy.mod.forge;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(TrueEconomyForge.MODID)
public class TrueEconomyForge {
    public static final String MODID = "trueeconomy";

    public TrueEconomyForge(net.minecraftforge.eventbus.api.IEventBus modBus) {
        TrueEconomyMod.init(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get(), new Adapter());
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onTick);
        modBus.addListener(this::onStopping);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        Commands.register(event.getDispatcher());
    }

    private void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MinecraftServer server = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerHooks.onEndTick(server);
            }
        }
    }

    private void onStopping(net.minecraftforge.fmlserverevents.FMLServerStoppingEvent event) {
        ServerHooks.onServerStopped();
    }
}