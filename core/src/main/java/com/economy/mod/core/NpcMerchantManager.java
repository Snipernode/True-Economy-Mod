package com.economy.mod.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Set of NPC traders with config base-margin / sensitivity. */
public final class NpcMerchantManager {
    private final List<Aimerchant> merchants = new CopyOnWriteArrayList<>();
    private final double baseMargin;
    private final double marginSensitivity;

    public NpcMerchantManager(double baseMargin, double marginSensitivity) {
        this.baseMargin = baseMargin;
        this.marginSensitivity = marginSensitivity;
    }

    public double getBaseMargin() { return baseMargin; }
    public double getMarginSensitivity() { return marginSensitivity; }

    public Aimerchant addMerchant(String name, double cash, double inventory) {
        Aimerchant m = new Aimerchant(name, cash, inventory);
        merchants.add(m);
        return m;
    }

    public List<Aimerchant> all() { return List.copyOf(merchants); }
}