package com.economy.mod.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Registry of player shops. */
public final class ShopManager {
    private final Map<String, PlayerShop> shops = new ConcurrentHashMap<>();
    private final int maxShopsPerPlayer;
    private final int maxEmployeesPerShop;

    public ShopManager(int maxShopsPerPlayer, int maxEmployeesPerShop) {
        this.maxShopsPerPlayer = maxShopsPerPlayer;
        this.maxEmployeesPerShop = maxEmployeesPerShop;
    }

    public int getMaxShopsPerPlayer() { return maxShopsPerPlayer; }
    public int getMaxEmployeesPerShop() { return maxEmployeesPerShop; }

    public int ownedBy(String owner) {
        return (int) shops.values().stream().filter(s -> s.getOwner().equals(owner)).count();
    }

    public PlayerShop create(String owner, String name, double defaultWage) {
        if (ownedBy(owner) >= maxShopsPerPlayer) return null;
        PlayerShop shop = new PlayerShop(owner, name, maxEmployeesPerShop, defaultWage);
        shops.put(shop.getName().toLowerCase(), shop);
        return shop;
    }

    public PlayerShop get(String name) { return shops.get(name.toLowerCase()); }
    public java.util.Collection<PlayerShop> all() { return shops.values(); }

    public void remove(String name) { shops.remove(name.toLowerCase()); }
}