package com.economy.mod.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Skims transaction tax into per-region city wallets; handles grants. */
public final class GovernmentManager {
    private final Map<String, CityWallet> wallets = new ConcurrentHashMap<>();
    private final double transactionTaxPercent;
    private final long grantCooldownMs;

    public GovernmentManager(double transactionTaxPercent, long grantCooldownMs) {
        this.transactionTaxPercent = transactionTaxPercent;
        this.grantCooldownMs = grantCooldownMs;
    }

    public double getTransactionTaxPercent() { return transactionTaxPercent; }
    public long getGrantCooldownMs() { return grantCooldownMs; }

    /** Amount taxed on a payment; credited to the region wallet. Returns tax taken. */
    public double levyTransactionTax(String region, double amount) {
        double tax = amount * transactionTaxPercent;
        if (tax > 0) wallet(region).deposit(tax);
        return tax;
    }

    public CityWallet wallet(String region) {
        return wallets.computeIfAbsent(region, CityWallet::new);
    }

    /** Grant from a region wallet to a player; returns amount granted or 0 on cooldown/funds. */
    public double grant(String region, double amount) {
        CityWallet w = wallet(region);
        if (w.onGrantCooldown(grantCooldownMs)) return 0;
        if (!w.withdraw(amount)) return 0;
        w.markGrant();
        return amount;
    }

    public Map<String, CityWallet> wallets() { return wallets; }
}