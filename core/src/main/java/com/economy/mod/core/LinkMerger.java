package com.economy.mod.core;

/** Merges one identity into another (currency, bank, credit, discoveries). */
public final class LinkMerger {
    private final EconomyManager economy;
    private final BankManager bank;
    private final ShopCatalog catalog;

    public LinkMerger(EconomyManager economy, BankManager bank, ShopCatalog catalog) {
        this.economy = economy;
        this.bank = bank;
        this.catalog = catalog;
    }

    public void merge(String primaryKey, String secondaryKey) {
        // canonical owners are resolved up-stream by the glue layer via the link maps
        catalog.mergeIdentities(primaryKey, secondaryKey);
    }

    public void mergeAccounts(String primaryOwner, String secondaryOwner) {
        if (primaryOwner.equals(secondaryOwner)) return;
        Account primary = economy.getAccount(primaryOwner);
        Account secondary = economy.getAccount(secondaryOwner);
        primary.setBalance(primary.getBalance() + secondary.getBalance());
        primary.setBankBalance(primary.getBankBalance() + secondary.getBankBalance());
        primary.setCreditScore(Math.max(primary.getCreditScore(), secondary.getCreditScore()));
    }
}