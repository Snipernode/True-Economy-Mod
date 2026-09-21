package com.economy.mod.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Loads commodities and runs the tick-driven price recalc. */
public final class MarketManager {
    private final Map<String, Commodity> items = new ConcurrentHashMap<>();
    private final List<Commodity> order = new ArrayList<>();
    private final double marketVolatility;
    private final String marketAccount;

    public MarketManager(double marketVolatility, String marketAccount) {
        this.marketVolatility = marketVolatility;
        this.marketAccount = marketAccount;
    }

    public void addCommodity(Commodity c) {
        items.put(c.getId(), c);
        order.add(c);
    }

    public Commodity get(String id) { return items.get(id.toLowerCase()); }
    public Collection<Commodity> all() { return order; }
    public double getVolatility() { return marketVolatility; }
    public String getMarketAccount() { return marketAccount; }

    public void tick() {
        for (Commodity c : order) c.recalculate(marketVolatility);
    }

    /** Buy units on the market against the market account balance. */
    public TradeResult buy(EconomyManager econ, String buyer, String commodityId, int units) {
        Commodity c = get(commodityId);
        if (c == null) return TradeResult.error("Unknown commodity: " + commodityId);
        double unitPrice = c.buy(units);
        double total = unitPrice * units;
        if (!econ.transfer(buyer, marketAccount, total)) {
            return TradeResult.error("Insufficient funds for " + econ.format(total) + ".");
        }
        return TradeResult.ok("Bought " + units + "x " + c.getName() + " for " + econ.format(total), c, unitPrice);
    }

    public TradeResult sell(EconomyManager econ, String seller, String commodityId, int units) {
        Commodity c = get(commodityId);
        if (c == null) return TradeResult.error("Unknown commodity: " + commodityId);
        double unitPrice = c.sell(units, marketVolatility);
        double total = unitPrice * units;
        econ.transfer(marketAccount, seller, total);
        return TradeResult.ok("Sold " + units + "x " + c.getName() + " for " + econ.format(total), c, unitPrice);
    }

    public static final class TradeResult {
        public final boolean ok;
        public final String message;
        public final Commodity commodity;
        public final double unitPrice;
        private TradeResult(boolean ok, String message, Commodity commodity, double unitPrice) {
            this.ok = ok; this.message = message; this.commodity = commodity; this.unitPrice = unitPrice;
        }
        static TradeResult ok(String msg, Commodity c, double price) { return new TradeResult(true, msg, c, price); }
        static TradeResult error(String msg) { return new TradeResult(false, msg, null, 0); }
    }
}