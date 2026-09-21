package com.economy.mod.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** JSON config with the plugin's defaults. Missing keys fall back automatically. */
public final class EconomyConfig {
    public static final class ItemDef { public String id; public String name; public double basePrice; public double elasticity; }
    public static final class SectionDef { public String id; public String display; public List<String> requiredBlocks = List.of(); public List<String> items = List.of(); }
    public static final class MerchantDef { public String name; public double cash; public double inventory; }
    public static final class LoanDef {
        public boolean enabled = true;
        public double defaultInterestPercent = 0.05;
        public double maxLoanAmount = 10000.0;
        public long repaymentPeriodSeconds = 604800L;
        public int defaultMinCreditScore = 300;
        public int defaultPenalty = 100;
        public int creditRepairPerRepayment = 5;
    }

    public static final class Root {
        public String symbol = "$";
        public int decimals = 2;
        public double startingBalance = 100.0;
        public double maxBalance = 1_000_000_000.0;
        public int defaultCredit = 500;

        public boolean marketEnabled = true;
        public long marketTickInterval = 300;
        public double marketVolatility = 0.03;
        public List<ItemDef> items = new ArrayList<>();
        public double marketHandlingFee = 0.0;

        public long auctionDefaultDuration = 300;
        public long auctionMaxDuration = 86400;
        public double minIncrementPercent = 0.05;
        public boolean buyItNowEnabled = true;
        public double listingFee = 5.0;
        public double salesTaxPercent = 0.02;

        public boolean bankingEnabled = true;
        public double interestRatePerCycle = 0.001;
        public long interestCycleSeconds = 600;
        public double maxAccountBalance = 1_000_000.0;
        public LoanDef loans = new LoanDef();

        public boolean npcEnabled = true;
        public double baseMargin = 0.3;
        public double marginSensitivity = 0.5;
        public List<MerchantDef> merchants = new ArrayList<>();

        public boolean shopsEnabled = true;
        public int maxShopsPerPlayer = 10;
        public int maxEmployeesPerShop = 5;
        public double defaultWage = 50.0;
        public List<SectionDef> sections = new ArrayList<>();

        public boolean governmentEnabled = true;
        public double transactionTaxPercent = 0.05;
        public boolean grantsEnabled = true;
        public int grantsCooldownDays = 7;

        public boolean seasonalEnabled = true;
        public long eventIntervalMinSeconds = 1800;
        public long eventIntervalMaxSeconds = 7200;
        public double crisisChance = 0.3;
        public double boomChance = 0.3;
        public double rareLootChance = 0.15;

        public String dataFile = "true-economy/data.json";
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Root load(Path file) {
        Root root = new Root();
        if (!Files.exists(file)) return root;
        try (Reader r = Files.newBufferedReader(file)) {
            Root loaded = GSON.fromJson(r, Root.class);
            if (loaded != null) root = loaded;
        } catch (IOException ignored) {
        }
        return root;
    }

    public static void save(Path file, Root root) {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root));
        } catch (IOException ignored) {
        }
    }

    private EconomyConfig() {}
}