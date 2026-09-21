package com.economy.mod.core;

import java.util.UUID;

/** An auction listing with escrowed bids, buy-it-now and expiry resolution. */
public final class Auction {
    public enum Status { ACTIVE, SOLD, EXPIRED, CLAIMED }

    private final UUID id = UUID.randomUUID();
    private final String seller;
    private final String itemId;
    private final long createdAt;
    private final long expiresAt;
    private final double startingBid;
    private final double buyItNowPrice;

    private String highestBidder;
    private double highestBid;
    private double escrow;
    private Status status = Status.ACTIVE;

    public Auction(String seller, String itemId, long durationMs, double startingBid, double buyItNowPrice) {
        this.seller = seller;
        this.itemId = itemId;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = createdAt + durationMs;
        this.startingBid = Math.max(0.01, startingBid);
        this.highestBid = 0;
        this.buyItNowPrice = buyItNowPrice;
    }

    public UUID getId() { return id; }
    public String getSeller() { return seller; }
    public String getItemId() { return itemId; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }
    public double getStartingBid() { return startingBid; }
    public double getBuyItNowPrice() { return buyItNowPrice; }
    public String getHighestBidder() { return highestBidder; }
    public double getHighestBid() { return highestBid; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public boolean isExpired() { return System.currentTimeMillis() > expiresAt; }

    /** Escrowed bid. Returns the refunded amount of the previous bidder (0 if none). */
    public double placeBid(String bidder, double amount, double minIncrementPercent) {
        if (!bidder.equals(seller) && amount <= 0) return -1;
        double minNext = highestBid == 0 ? startingBid : highestBid * (1.0 + minIncrementPercent);
        if (!bidder.equals(seller) && amount < minNext) return -1;
        double refund = escrow;
        highestBidder = bidder;
        highestBid = amount;
        escrow = amount;
        return refund;
    }

    /** Buy-it-now. Returns the escrow refund to refund to the previous highest bidder. */
    public double buyNow(String buyer, double fee) {
        if (buyItNowPrice <= 0 || status != Status.ACTIVE) return -1;
        double refund = escrow;
        highestBidder = buyer;
        highestBid = buyItNowPrice;
        escrow = buyItNowPrice - fee;
        status = Status.SOLD;
        return refund;
    }

    /** Winner payment minus fees => seller. */
    public void finalizeSale(String winner) {
        if (status == Status.SOLD || winner == null) return;
        if (highestBidder == null || !highestBidder.equals(winner)) return;
        status = Status.SOLD;
    }
}