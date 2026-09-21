package com.economy.mod.core;

/**
 * A marketable commodity. Price drifts toward its base over time; buying raises
 * demand, selling floods supply. Volatility scales how hard volume moves price.
 */
public final class Commodity {
    private final String id;
    private final String name;
    private final double basePrice;
    private final double elasticity;

    private double supply = 100.0;
    private double demand = 50.0;
    private double currentPrice;
    private long lastMovement = System.currentTimeMillis();

    public Commodity(String id, String name, double basePrice, double elasticity) {
        this.id = id;
        this.name = name;
        this.basePrice = Math.max(0.01, basePrice);
        this.elasticity = Math.max(0.1, elasticity);
        this.currentPrice = this.basePrice;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getBasePrice() { return basePrice; }
    public double getElasticity() { return elasticity; }
    public double getSupply() { return supply; }
    public double getDemand() { return demand; }

    private static double capPrice(double p) { return Math.max(0.01, p); }

    /** Price with decay toward base: 1% of the gap decays per elapsed minute. */
    public double getCurrentPrice() {
        long now = System.currentTimeMillis();
        double minutes = Math.max(0, (now - lastMovement) / 60000.0);
        double factor = Math.max(0.0, Math.min(1.0, 1.0 - minutes * 0.01));
        currentPrice = capPrice(basePrice + (currentPrice - basePrice) * factor);
        return currentPrice;
    }

    private void move(double marketVolatility) {
        lastMovement = System.currentTimeMillis();
        double pressure = Math.max(-1.0, Math.min(1.0, (demand - supply) / 100.0));
        currentPrice = capPrice(currentPrice * (1.0 + pressure * elasticity * marketVolatility));
    }

    /** Player buys `units` from the market: demand up, slight supply drawdown. */
    public double buy(int units) {
        getCurrentPrice();
        double price = currentPrice;
        demand += units;
        supply -= units * 0.05;
        if (supply < 1.0) supply = 1.0;
        move(0.03);
        return price;
    }

    /** Player sells `units` to the market: supply up, small demand bleed. */
    public double sell(int units, double marketVolatility) {
        getCurrentPrice();
        double price = currentPrice;
        supply += units;
        demand -= units * 0.2;
        if (demand < 1.0) demand = 1.0;
        move(marketVolatility);
        return price;
    }

    /** Scheduled recalc with arbitrary volatility. */
    public void recalculate(double marketVolatility) {
        getCurrentPrice();
        double pressure = Math.max(-1.0, Math.min(1.0, (demand - supply) / 100.0));
        currentPrice = capPrice(currentPrice * (1.0 + pressure * elasticity * marketVolatility));
    }

    /** Seasonal event temp multiplier overlay set by SeasonalEventManager. */
    public double withModifier(double multiplier) {
        return capPrice(getCurrentPrice() * multiplier);
    }
}