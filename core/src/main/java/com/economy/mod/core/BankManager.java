package com.economy.mod.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Banking: dep/withdraw, per-cycle interest, loans gated by credit score. */
public final class BankManager {
    private final double interestRatePerCycle;
    private final long interestCycleMs;
    private final double maxAccountBalance;
    private final boolean loansEnabled;
    private final double defaultInterestPercent;
    private final double maxLoanAmount;
    private final long repaymentPeriodMs;
    private final int defaultMinCreditScore;
    private final int defaultPenalty;
    private final int creditRepairPerRepayment;

    private final Map<String, List<Loan>> loansByOwner = new ConcurrentHashMap<>();
    private long lastInterest = System.currentTimeMillis();

    public BankManager(double interestRatePerCycle, long interestCycleMs, double maxAccountBalance,
                       boolean loansEnabled, double defaultInterestPercent, double maxLoanAmount,
                       long repaymentPeriodMs, int defaultMinCreditScore, int defaultPenalty,
                       int creditRepairPerRepayment) {
        this.interestRatePerCycle = interestRatePerCycle;
        this.interestCycleMs = interestCycleMs;
        this.maxAccountBalance = maxAccountBalance;
        this.loansEnabled = loansEnabled;
        this.defaultInterestPercent = defaultInterestPercent;
        this.maxLoanAmount = maxLoanAmount;
        this.repaymentPeriodMs = repaymentPeriodMs;
        this.defaultMinCreditScore = defaultMinCreditScore;
        this.defaultPenalty = defaultPenalty;
        this.creditRepairPerRepayment = creditRepairPerRepayment;
    }

    public double getInterestRatePerCycle() { return interestRatePerCycle; }
    public long getInterestCycleMs() { return interestCycleMs; }
    public double getMaxAccountBalance() { return maxAccountBalance; }
    public boolean isLoansEnabled() { return loansEnabled; }
    public double getDefaultInterestPercent() { return defaultInterestPercent; }
    public double getMaxLoanAmount() { return maxLoanAmount; }
    public long getRepaymentPeriodMs() { return repaymentPeriodMs; }
    public int getDefaultMinCreditScore() { return defaultMinCreditScore; }

    public boolean isLoanAllowed(int creditScore) { return creditScore >= defaultMinCreditScore; }

    public List<Loan> getLoans(String owner) {
        return new ArrayList<>(loansByOwner.computeIfAbsent(owner, k -> new ArrayList<>()));
    }

    public boolean hasDefaulted(String owner) {
        return loansByOwner.getOrDefault(owner, List.of()).stream().anyMatch(l -> l.getStatus() == Loan.Status.DEFAULTED);
    }

    public Loan createLoan(String owner, double amount, int creditScore) {
        if (!loansEnabled) throw new IllegalArgumentException("Loans are disabled.");
        if (!isLoanAllowed(creditScore)) throw new IllegalArgumentException("Credit score too low.");
        if (amount <= 0 || amount > maxLoanAmount) throw new IllegalArgumentException("Loan amount out of range.");
        Loan loan = new Loan(owner, amount, defaultInterestPercent, repaymentPeriodMs);
        loansByOwner.computeIfAbsent(owner, k -> new ArrayList<>()).add(loan);
        return loan;
    }

    /** True if fully repaid. False on insufficient funds/not active. */
    public boolean repay(String owner, double amount) {
        double outstanding = remainingActive(owner);
        if (outstanding <= 0) return true;
        int scoreBefore = scoreCache.getOrDefault(owner, defaultMinCreditScore + 200);
        for (Loan loan : loansByOwner.getOrDefault(owner, List.of())) {
            if (loan.getStatus() != Loan.Status.ACTIVE) continue;
            double slice = Math.min(loan.getRemaining(), amount);
            if (!loan.repay(slice)) continue;
            amount -= slice;
            if (amount <= 0) break;
        }
        double after = remainingActive(owner);
        boolean fullyPaid = after <= 0;
        if (fullyPaid || scoreCache.getOrDefault(owner, 0) > 0) {
            bumpCredit(owner, creditRepairPerRepayment);
        }
        return fullyPaid;
    }

    private double remainingActive(String owner) {
        double sum = 0;
        for (Loan l : loansByOwner.getOrDefault(owner, List.of()))
            if (l.getStatus() == Loan.Status.ACTIVE) sum += l.getRemaining();
        return sum;
    }

    private final Map<String, Integer> scoreCache = new ConcurrentHashMap<>();
    void setCredit(String owner, int score) { scoreCache.put(owner, score); }
    private void bumpCredit(String owner, int amount) {
        scoreCache.merge(owner, amount, Integer::sum);
    }

    public int creditDelta(String owner) { return scoreCache.getOrDefault(owner, 0); }

    /** Called on a cycle tick: apply interest, then default overdue loans. */
    public void onCycle(Map<String, Account> accounts) {
        long now = System.currentTimeMillis();
        if (now - lastInterest < interestCycleMs) return;
        lastInterest = now;
        for (Account a : accounts.values()) {
            a.applyInterest(interestRatePerCycle);
            if (a.getBankBalance() > maxAccountBalance) a.setBankBalance(maxAccountBalance);
        }
        for (Map.Entry<String, List<Loan>> e : loansByOwner.entrySet()) {
            for (Loan loan : e.getValue()) {
                if (loan.getStatus() == Loan.Status.ACTIVE && loan.isExpired()) {
                    loan.setDefaulted();
                }
            }
        }
    }
}