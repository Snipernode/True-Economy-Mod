package com.economy.mod.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/** Owns all accounts and the money transfer/pay primitive (with optional tax). */
public final class EconomyManager {
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final double startingBalance;
    private final int defaultCredit;
    private final String symbol;
    private final int decimals;
    private BiConsumer<String, Double> taxSink = (region, amount) -> { };

    public EconomyManager(double startingBalance, int defaultCredit, String symbol, int decimals) {
        this.startingBalance = startingBalance;
        this.defaultCredit = defaultCredit;
        this.symbol = symbol;
        this.decimals = decimals;
    }

    public Account getAccount(String owner) {
        return accounts.computeIfAbsent(owner, k -> new Account(k, startingBalance, defaultCredit));
    }

    public Map<String, Account> accounts() { return accounts; }

    public double getStartingBalance() { return startingBalance; }
    public int getDefaultCredit() { return defaultCredit; }

    public void setTaxSink(BiConsumer<String, Double> sink) { this.taxSink = sink; }

    /** Transfers money owner->to; returns false if payer cannot afford it. */
    public boolean transfer(String owner, String to, double amount) {
        if (amount <= 0) return false;
        Account from = getAccount(owner);
        if (!from.withdraw(amount)) return false;
        getAccount(to).deposit(amount);
        return true;
    }

    public String format(double amount) {
        return String.format("%s%,." + decimals + "f", symbol, amount);
    }

    public void restore(String owner, double balance, double bank, int credit) {
        Account a = getAccount(owner);
        a.setBalance(balance);
        a.setBankBalance(bank);
        a.setCreditScore(credit);
    }

    public void clear() { accounts.clear(); }
}