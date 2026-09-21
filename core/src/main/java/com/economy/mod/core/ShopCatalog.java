package com.economy.mod.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Catalog of shop sections + discovery registry per identity key. */
public final class ShopCatalog {
    private final List<ShopSection> sections = new java.util.ArrayList<>();
    private final Map<String, java.util.Set<String>> discoveriesById = new ConcurrentHashMap<>();

    public void addSection(ShopSection s) { sections.add(s); }
    public List<ShopSection> all() { return List.copyOf(sections); }

    public java.util.Set<String> discovered(String identityKey) {
        return discoveriesById.computeIfAbsent(identityKey, k -> java.util.Collections.synchronizedSet(new java.util.LinkedHashSet<>()));
    }

    Map<String, java.util.Set<String>> discoveriesById() { return discoveriesById; }

    public void record(String identityKey, String materialName) {
        discovered(identityKey).add(materialName.toLowerCase());
    }

    public List<ShopSection> unlocked(String identityKey) {
        java.util.Set<String> set = discovered(identityKey);
        return sections.stream().filter(s -> s.isUnlocked(set)).collect(java.util.stream.Collectors.toList());
    }

    /** Merge a secondary identity into primary (duplicates collapse, order preserved). */
    public void mergeIdentities(String primary, String secondary) {
        if (primary.equals(secondary)) return;
        discovered(primary).addAll(discovered(secondary));
        discoveriesById.remove(secondary);
    }
}