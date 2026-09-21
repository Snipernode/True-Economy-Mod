package com.economy.mod.core;

/** Government wallet: taxes collect into it, grants pay back out. */
public final class CityWallet {
    private final String region;
    private double balance = 0.0;
    private long lastGrant = 0;

    public CityWallet(String region) { this.region = region; }
    public String getRegion() { return region; }
    public double getBalance() { return balance; }
    public void deposit(double amount) { balance += amount; }
    public boolean withdraw(double amount) {
        if (amount <= 0 || balance + 1e-9 < amount) return false;
        balance -= amount;
        return true;
    }
    public boolean onGrantCooldown(long cooldownMs) { return System.currentTimeMillis() - lastGrant < cooldownMs; }
    public void markGrant() { lastGrant = System.currentTimeMillis(); }
}