package com.economy.mod.fabric;

import com.economy.mod.core.EconomyConfig;
import com.economy.mod.core.EconomyCore;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/** Static holder for the running economy instance. */
public final class TrueEconomyMod {
    public static EconomyCore core;
    public static EconomyConfig.Root config;

    private TrueEconomyMod() {}

    public static synchronized void init() {
        if (core != null) return;
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path cfgFile = configDir.resolve("true-economy.json");
        config = EconomyConfig.load(cfgFile);
        Path dataFile = configDir.resolve(config.dataFile);
        core = new EconomyCore(config, dataFile);
        if (!java.nio.file.Files.exists(cfgFile)) EconomyConfig.save(cfgFile, config);
        TrueEconomyMod.log("True-Economy loaded: %d commodities, %d sections, %d merchants",
                config.items.size(), config.sections.size(), config.merchants.size());
    }

    public static EconomyCore core() { return core; }

    public static void log(String fmt, Object... args) {
        System.out.println("[True-Economy] " + String.format(fmt, args));
    }
}