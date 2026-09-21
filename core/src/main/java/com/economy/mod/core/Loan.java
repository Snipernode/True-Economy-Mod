package com.economy.mod.core;

/** A bank loan with interest, due date and repayment/credit behavior. */
public final class Loan {
    public enum Status { ACTIVE, PAID, DEFAULTED }

    private final String owner;
    private final double amount;
    private final double interestPercent;
    private final long grantedAt;
    private final long dueAt;
    private double remaining;
    private Status status = Status.ACTIVE;

    public Loan(String owner, double amount, double interestPercent, long repaymentPeriodMs) {
        this.owner = owner;
        this.amount = amount;
        this.interestPercent = interestPercent;
        this.grantedAt = System.currentTimeMillis();
        this.dueAt = grantedAt + repaymentPeriodMs;
        this.remaining = amount * (1.0 + interestPercent);
    }

    public String getOwner() { return owner; }
    public double getAmount() { return amount; }
    public double getInterestPercent() { return interestPercent; }
    public long getGrantedAt() { return grantedAt; }
    public long getDueAt() { return dueAt; }
    public double getRemaining() { return remaining; }
    public Status getStatus() { return status; }
    public boolean isExpired() { return System.currentTimeMillis() > dueAt; }

    public boolean repay(double amount) {
        if (status != Status.ACTIVE || amount <= 0) return false;
        double paid = Math.min(remaining, amount);
        double change = remaining - paid;
        remaining = change < 1e-9 ? 0.0 : change;
        if (remaining <= 0) status = Status.PAID;
        return paid > 0;
    }

    public void setDefaulted() { if (status == Status.ACTIVE) status = Status.DEFAULTED; }
}