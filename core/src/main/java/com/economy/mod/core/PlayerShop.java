package com.economy.mod.core;

import java.util.List;

/** A player-owned shop: up to 5 hired employees earning wages and producing revenue. */
public final class PlayerShop {
    private final String owner;
    private final String name;
    private final int maxEmployees;
    private final double defaultWage;
    private final java.util.List<String> employees = new java.util.ArrayList<>();
    private double wage;
    private double revenueCollected;

    public PlayerShop(String owner, String name, int maxEmployees, double defaultWage) {
        this.owner = owner;
        this.name = name;
        this.maxEmployees = maxEmployees;
        this.defaultWage = defaultWage;
        this.wage = defaultWage;
    }

    public String getOwner() { return owner; }
    public String getName() { return name; }
    public int getMaxEmployees() { return maxEmployees; }
    public double getWage() { return wage; }
    public void setWage(double wage) { this.wage = Math.max(0, wage); }
    public List<String> getEmployees() { return List.copyOf(employees); }
    public double getRevenueCollected() { return revenueCollected; }

    public boolean hire(String employee) {
        if (employees.size() >= maxEmployees || employees.contains(employee)) return false;
        return employees.add(employee);
    }

    public boolean fire(String employee) { return employees.removeIf(employee::equals); }

    /** Run one production cycle: employees generate revenue; wages are paid out of it. */
    public double produceCycle() {
        double revenue = employees.size() * 4.0;
        double wages = employees.size() * wage;
        revenueCollected += revenue;
        return revenue - wages;
    }
}