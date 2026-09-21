package com.economy.mod.core;

/** A player's wallet, bank balance and credit score. */
public final class Account {
    private final String owner;
    private double balance;
    private double bankBalance;
    private int creditScore;

    public Account(String owner, double startingBalance, int defaultCredit) {
        this.owner = owner;
        this.balance = startingBalance;
        this.bankBalance = 0.0;
        this.creditScore = defaultCredit;
    }

    public String getOwner() { return owner; }
    public double getBalance() { return balance; }
    public double getBankBalance() { return bankBalance; }
    public int getCreditScore() { return creditScore; }

    public void modifyBalance(double delta) { this.balance += delta; }
    public void setBalance(double b) { this.balance = b; }
    public void setBankBalance(double b) { this.bankBalance = b; }
    public void setCreditScore(int c) { this.creditScore = Math.max(0, c); }

    public boolean deposit(double amount) {
        if (amount <= 0) return false;
        this.balance += amount;
        return true;
    }

    public boolean withdraw(double amount) {
        if (amount <= 0 || this.balance + 1e-9 < amount) return false;
        this.balance -= amount;
        return true;
    }

    public boolean depositBank(double amount) {
        if (amount <= 0 || this.balance + 1e-9 < amount) return false;
        this.balance -= amount;
        this.bankBalance += amount;
        return true;
    }

    public boolean withdrawBank(double amount) {
        if (amount <= 0 || this.bankBalance + 1e-9 < amount) return false;
        this.bankBalance -= amount;
        this.balance += amount;
        return true;
    }

    public void applyInterest(double ratePerCycle) {
        this.bankBalance += this.bankBalance * ratePerCycle;
    }
}