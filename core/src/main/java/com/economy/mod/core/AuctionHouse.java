package com.economy.mod.core;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Holds active + resolved auctions and resolves expirations. */
public final class AuctionHouse {
    private final Map<UUID, Auction> auctions = new ConcurrentHashMap<>();
    private final Map<UUID, Auction> resolved = new ConcurrentHashMap<>();
    private final double minIncrementPercent;
    private final double salesTax;
    private final double listingFee;
    private final java.util.function.Function<String, String> ownerResolver;

    public AuctionHouse(double minIncrementPercent, double salesTax, double listingFee,
                        java.util.function.Function<String, String> ownerResolver) {
        this.minIncrementPercent = minIncrementPercent;
        this.salesTax = salesTax;
        this.listingFee = listingFee;
        this.ownerResolver = ownerResolver == null ? java.util.function.Function.identity() : ownerResolver;
    }

    public void add(Auction a) { auctions.put(a.getId(), a); }
    public Auction get(UUID id) { return auctions.get(id); }
    public List<Auction> getActive() { return auctions.values().stream().filter(a -> a.getStatus() == Auction.Status.ACTIVE).collect(Collectors.toList()); }
    public List<Auction> getAll() { return List.copyOf(auctions.values()); }
    public List<Auction> getAllResolved() { return List.copyOf(resolved.values()); }
    public List<Auction> getBySeller(String seller) {
        String canon = ownerResolver.apply(seller);
        return getActive().stream().filter(a -> ownerResolver.apply(a.getSeller()).equals(canon)).collect(Collectors.toList());
    }
    public double getMinIncrementPercent() { return minIncrementPercent; }
    public double getSalesTax() { return salesTax; }
    public double getListingFee() { return listingFee; }
    public void remove(UUID id) { auctions.remove(id); }

    public boolean placeBid(Auction a, String bidder, double amount) {
        return a != null && a.placeBid(ownerResolver.apply(bidder), amount, minIncrementPercent) >= 0;
    }

    /** Buy-it-now. Refunds the previous escrow holder; returns the refund or -1. */
    public double buyNow(Auction a, String buyer, double fee) {
        if (a == null) return -1;
        return a.buyNow(ownerResolver.apply(buyer), fee);
    }

    public List<Auction> processExpirations() {
        List<Auction> finalized = new java.util.ArrayList<>();
        for (Auction a : List.copyOf(auctions.values())) {
            if (a.getStatus() != Auction.Status.ACTIVE || !a.isExpired()) continue;
            if (a.getHighestBidder() != null) a.finalizeSale(a.getHighestBidder());
            else a.setStatus(Auction.Status.EXPIRED);
            auctions.remove(a.getId());
            resolved.put(a.getId(), a);
            finalized.add(a);
        }
        return finalized;
    }
}