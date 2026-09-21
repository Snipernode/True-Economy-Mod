package com.economy.mod.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Random crisis/boom market events with price modifiers on commodities. */
public final class SeasonalEventManager {
    public enum EventType { CRISIS, BOOM, EVENT }

    public static final class MarketEvent {
        public final EventType type;
        public final String commodityId;
        public final double multiplier;
        public final long endsAt;

        MarketEvent(EventType type, String commodityId, double multiplier, long durationMs) {
            this.type = type;
            this.commodityId = commodityId;
            this.multiplier = multiplier;
            this.endsAt = System.currentTimeMillis() + durationMs;
        }

        public boolean isActive() { return System.currentTimeMillis() < endsAt; }
    }

    private final List<MarketEvent> history = new ArrayList<>();
    private final List<MarketEvent> active = new ArrayList<>();
    private final Random random = new Random();
    private final double crisisChance;
    private final double boomChance;
    private final double rareLootChance;
    private final long minIntervalMs;
    private final long maxIntervalMs;
    private static final int HISTORY_LIMIT = 25;

    private long nextEventAt = System.currentTimeMillis() + nextDelayMs();

    public SeasonalEventManager(double crisisChance, double boomChance, double rareLootChance,
                                long minIntervalMs, long maxIntervalMs) {
        this.crisisChance = crisisChance;
        this.boomChance = boomChance;
        this.rareLootChance = rareLootChance;
        this.minIntervalMs = minIntervalMs;
        this.maxIntervalMs = maxIntervalMs;
    }

    private long nextDelayMs() {
        long span = Math.max(1000, maxIntervalMs - minIntervalMs);
        return minIntervalMs + (long) (random.nextDouble() * span);
    }

    private EventType rollType() {
        double r = random.nextDouble();
        if (r < crisisChance) return EventType.CRISIS;
        if (r < crisisChance + boomChance) return EventType.BOOM;
        return EventType.EVENT;
    }

    /** Consumer receives NewEvent payload. */
    public MarketEvent rollNext(List<Commodity> commodities) {
        if (commodities.isEmpty()) return null;
        EventType type = rollType();
        Commodity target = commodities.get(random.nextInt(commodities.size()));
        double multiplier;
        switch (type) {
            case CRISIS: multiplier = 0.5 + random.nextDouble() * 0.3; break;
            case BOOM: multiplier = 1.5 + random.nextDouble() * 1.0; break;
            default: multiplier = 0.8 + random.nextDouble() * 0.6; break;
        }
        long duration = 60000L + (long) (random.nextDouble() * 300000L);
        MarketEvent event = new MarketEvent(type, target.getId(), multiplier, duration);
        active.add(event);
        history.add(0, event);
        if (history.size() > HISTORY_LIMIT) history.remove(history.size() - 1);
        return event;
    }

    public MarketEvent compactPrev() {
        if (history.isEmpty()) return null;
        MarketEvent last = history.get(0);
        active.removeIf(e -> !e.isActive());
        return last;
    }

    public void tick(List<Commodity> commodities) {
        active.removeIf(e -> !e.isActive());
        if (System.currentTimeMillis() >= nextEventAt) {
            rollNext(commodities);
            nextEventAt = System.currentTimeMillis() + nextDelayMs();
        }
    }

    public double modifierFor(String commodityId) {
        double m = 1.0;
        for (MarketEvent e : active) {
            if (e.isActive() && e.commodityId.equals(commodityId)) m *= e.multiplier;
        }
        return m;
    }

    public List<MarketEvent> getActiveEvents() { return List.copyOf(active); }
    public List<MarketEvent> getHistory() { return List.copyOf(history); }
    public long getNextEventTime() { return nextEventAt; }
}