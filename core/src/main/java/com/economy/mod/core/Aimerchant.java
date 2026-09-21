package com.economy.mod.core;

/** NPC trader: prices from market price, scarcity and margin sensitivity. */
public final class Aimerchant {
    private final String name;
    private double cashReserve;
    private double inventoryLevel;

    public Aimerchant(String name, double startingCash, double startingInventory) {
        this.name = name;
        this.cashReserve = startingCash;
        this.inventoryLevel = startingInventory;
    }

    public String getName() { return name; }
    public double getCashReserve() { return cashReserve; }
    public double getInventoryLevel() { return inventoryLevel; }
    public synchronized void earn(double amount) { this.cashReserve += amount; }
    public synchronized void spend(double amount) {
        if (this.cashReserve < amount) amount = this.cashReserve;
        this.cashReserve -= amount;
    }

    /** Offer NPC pays the player: floor of price*0.7 * scarcity (min 0.2x). */
    public double getBuyPrice(Commodity commodity, double baseMargin, double sensitivity) {
        double marketPrice = commodity.getCurrentPrice();
        double scarcityFactor = Math.max(0.2, 100.0 / commodity.getSupply());
        double margin = baseMargin * sensitivity + (1.0 - sensitivity) * 0.15;
        double offer = marketPrice * 0.7 * scarcityFactor * (1.0 - margin);
        return Math.max(0.01, offer);
    }

    /** Price NPC sells to the player. */
    public double getSellPrice(Commodity commodity, double baseMargin, double sensitivity) {
        double marketPrice = commodity.getCurrentPrice();
        double margin = baseMargin * sensitivity + (1.0 - sensitivity) * 0.5;
        return marketPrice * (1.0 + margin);
    }
}