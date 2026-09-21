package com.economy.mod.forge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(TrueEconomyForge.MODID)
public class TrueEconomyForge {
    public static final String MODID = "trueeconomy";

    public TrueEconomyForge() {
        TrueEconomyMod.init(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get(), new Adapter());
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onTick);
        MinecraftForge.EVENT_BUS.addListener(this::onStopping);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        Commands.register(dispatcher);
    }

    private void onTick(TickEvent.ServerTickEvent event) {
        if (event.getServer() != null) {
            ServerHooks.onEndTick(event.getServer());
        }
    }

    private void onStopping(ServerStoppingEvent event) {
        ServerHooks.onServerStopped();
    }
}
