package com.economy.mod.core;

import java.util.List;

/** One black-market section. Unlocked only when every required block is discovered. */
public final class ShopSection {
    private final String id;
    private final String display;
    private final List<String> requiredBlocks;
    private final List<String> itemIds;

    public ShopSection(String id, String display, List<String> requiredBlocks, List<String> itemIds) {
        this.id = id;
        this.display = display;
        this.requiredBlocks = List.copyOf(requiredBlocks);
        this.itemIds = List.copyOf(itemIds);
    }

    public String getId() { return id; }
    public String getDisplay() { return display; }
    public List<String> getRequiredBlocks() { return requiredBlocks; }
    public List<String> getItemIds() { return itemIds; }

    public boolean isUnlocked(java.util.Set<String> discoveredBlocks) {
        return discoveredBlocks.containsAll(requiredBlocks);
    }
}