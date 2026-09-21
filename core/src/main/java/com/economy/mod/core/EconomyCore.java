package com.economy.mod.core;

import java.util.function.Function;

/** Top-level facade wiring every subsystem together. Glue calls tick()/save() from game ticks. */
public final class EconomyCore {
    public static final String MARKET_ACCOUNT = "__market__";

    private final EconomyConfig.Root config;
    private final EconomyManager economy;
    private final MarketManager market;
    private final BankManager bank;
    private final AuctionHouse auctions;
    private final ShopManager shops;
    private final ShopCatalog catalog;
    private final GovernmentManager government;
    private final SeasonalEventManager seasonal;
    private final NpcMerchantManager npc;
    private final LinkManager links;
    private final DataStore dataStore;

    private Function<String, String> identityKeyResolver = key -> key;
    private Function<String, String> ownerResolver = owner -> owner;

    private long lastTick = System.currentTimeMillis();
    private long lastSave = System.currentTimeMillis();

    public EconomyCore(EconomyConfig.Root cfg, java.nio.file.Path dataFile) {
        this.config = cfg;
        this.economy = new EconomyManager(cfg.startingBalance, cfg.defaultCredit, cfg.symbol, cfg.decimals);
        this.market = new MarketManager(cfg.marketVolatility, MARKET_ACCOUNT);
        this.bank = new BankManager(cfg.interestRatePerCycle, cfg.interestCycleSeconds * 1000L,
                cfg.maxAccountBalance, cfg.bankingEnabled && cfg.loans.enabled, cfg.loans.defaultInterestPercent,
                cfg.loans.maxLoanAmount, cfg.loans.repaymentPeriodSeconds * 1000L,
                cfg.loans.defaultMinCreditScore, cfg.loans.defaultPenalty, cfg.loans.creditRepairPerRepayment);
        this.auctions = new AuctionHouse(cfg.minIncrementPercent, cfg.salesTaxPercent, cfg.listingFee, s -> ownerResolver.apply(s));
        this.shops = new ShopManager(cfg.maxShopsPerPlayer, cfg.maxEmployeesPerShop);
        this.catalog = new ShopCatalog();
        this.government = new GovernmentManager(cfg.transactionTaxPercent, cfg.grantsCooldownDays * 86400000L);
        this.seasonal = new SeasonalEventManager(cfg.crisisChance, cfg.boomChance, cfg.rareLootChance,
                cfg.eventIntervalMinSeconds * 1000L, cfg.eventIntervalMaxSeconds * 1000L);
        this.npc = new NpcMerchantManager(cfg.baseMargin, cfg.marginSensitivity);
        this.links = new LinkManager(new LinkMerger(economy, bank, catalog));
        this.dataStore = new DataStore(dataFile);

        if (cfg.items != null) for (EconomyConfig.ItemDef d : cfg.items) {
            if (d == null || d.id == null) continue;
            market.addCommodity(new Commodity(d.id, d.name == null ? d.id : d.name, d.basePrice, d.elasticity));
        }
        if (cfg.sections != null) for (EconomyConfig.SectionDef s : cfg.sections) {
            if (s == null || s.id == null) continue;
            catalog.addSection(new ShopSection(s.id, s.display == null ? s.id : s.display, s.requiredBlocks, s.items));
        }
        if (cfg.merchants != null) for (EconomyConfig.MerchantDef m : cfg.merchants) {
            npc.addMerchant(m.name == null ? "Merchant" : m.name, m.cash, m.inventory);
        }
        if (cfg.marketEnabled) market.addCommodity(new Commodity("ancient-artifact", "Ancient Artifact", 1200, 2.5));

        economy.setTaxSink((region, amount) -> government.levyTransactionTax("default", amount));
        dataStore.loadInto(this);
    }

    public EconomyManager economy() { return economy; }
    public MarketManager market() { return market; }
    public BankManager bank() { return bank; }
    public AuctionHouse auctions() { return auctions; }
    public ShopManager shops() { return shops; }
    public ShopCatalog catalog() { return catalog; }
    public GovernmentManager government() { return government; }
    public SeasonalEventManager seasonal() { return seasonal; }
    public NpcMerchantManager npc() { return npc; }
    public LinkManager links() { return links; }
    public DataStore dataStore() { return dataStore; }
    public EconomyConfig.Root config() { return config; }

    public void setIdentityKeyResolver(Function<String, String> identityKeyResolver) {
        this.identityKeyResolver = identityKeyResolver;
        links.lookupMaps(new java.util.HashMap<>(), new java.util.HashMap<>()); // (maps re-fed by glue on demand)
    }

    /** The canonical identity key for a raw identity key (UUID/XUID). */
    public String canonicalIdentity(String raw) {
        return links.canonicalKey(raw);
    }

    /** Owner name used as the money key (post-link canonicalisation). */
    public String canonicalOwner(String owner) {
        return ownerResolver.apply(owner);
    }

    public void setOwnerResolver(Function<String, String> resolver) { this.ownerResolver = resolver; }

    /** Called every game tick (20/s) by the mod glue. */
    public void tick() {
        long now = System.currentTimeMillis();
        if (config.marketEnabled && now - lastTick >= config.marketTickInterval * 1000L) {
            lastTick = now;
            market.tick();
            bank.onCycle(economy.accounts());
            if (config.seasonalEnabled) seasonal.tick(new java.util.ArrayList<>(market.all()));
            for (PlayerShop s : shops.all()) {
                double profit = s.produceCycle();
                economy.getAccount(s.getOwner()).modifyBalance(profit);
            }
        }
        if (config.auctionDefaultDuration >= 0) {
            for (Auction a : auctions.processExpirations()) {
                if (a.getHighestBidder() == null) continue;
                double tax = a.getHighestBid() * auctions.getSalesTax();
                government.levyTransactionTax("default", tax);
                economy.getAccount(a.getSeller()).modifyBalance(a.getHighestBid() - tax);
                economy.getAccount(a.getHighestBidder()).withdraw(a.getHighestBid());
            }
        }
        if (now - lastSave >= 30000L) {
            lastSave = now;
            save();
        }
    }

    public void save() { dataStore.save(this); }

    public void shutdown() { save(); }

    /** Money key for a player by identity (UUID). */
    public String moneyKeyFor(java.util.UUID uuid, String mcName) {
        return ownerResolver.apply(mcName);
    }
}