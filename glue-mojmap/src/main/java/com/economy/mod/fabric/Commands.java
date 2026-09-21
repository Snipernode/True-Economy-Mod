package com.economy.mod.fabric;

import com.economy.mod.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Full economy command surface, rendered through the mod (chat-based UI v1).
 * Parsed as a single {@code /economy ...} with string args - stable across versions.
 */
public final class Commands {
    private Commands() {}

    public static void run(ServerPlayer player, String[] args) {
        if (args == null || args.length == 0) {
            help(player);
            return;
        }
        if (!TrueEconomyMod.core().config().governmentEnabled && "pay".equalsIgnoreCase(args[0])) {
            Commands.error(player, "Payments are disabled.");
            return;
        }
        String owner = player.getGameProfile().name();
        String identity = "uuid:" + player.getUUID();
        EconomyCore core = TrueEconomyMod.core();

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "balance": case "bal": balance(player, owner); break;
            case "pay": pay(player, owner, args); break;
            case "market": market(player, owner, args); break;
            case "auction": case "ah": auction(player, owner, args); break;
            case "bank": case "loan": bank(player, owner, args); break;
            case "shop": shop(player, owner, identity, args); break;
            case "events": events(player, args); break;
            case "link": link(player, owner, identity, args); break;
            default: help(player);
        }
    }

    // ---- helpers ----
    static void reply(ServerPlayer p, String msg) { p.sendSystemMessage(Component.literal(msg)); }
    static void error(ServerPlayer p, String msg) { p.sendSystemMessage(Component.literal("[!] " + msg)); }
    private static double num(String[] args, int idx, double fallback) {
        if (idx >= args.length) return fallback;
        try { return Double.parseDouble(args[idx]); } catch (NumberFormatException e) { return fallback; }
    }
    private static int numI(String[] args, int idx, int fallback) {
        if (idx >= args.length) return fallback;
        try { return Integer.parseInt(args[idx]); } catch (NumberFormatException e) { return fallback; }
    }

    private static void help(ServerPlayer p) {
        reply(p, "[True-Economy] /economy <balance|pay|market|auction|bank|shop|events|link>");
    }

    private static void balance(ServerPlayer p, String owner) {
        Account acc = TrueEconomyMod.core().economy().getAccount(owner);
        reply(p, "Balance: " + TrueEconomyMod.core().economy().format(acc.getBalance())
                + " (Bank: " + TrueEconomyMod.core().economy().format(acc.getBankBalance())
                + ")  Credit: " + acc.getCreditScore());
    }

    private static void pay(ServerPlayer p, String owner, String[] args) {
        if (args.length < 3) { error(p, "Usage: /economy pay <player> <amount>"); return; }
        double amount = num(args, 2, -1);
        if (amount <= 0) { error(p, "Invalid amount."); return; }
        EconomyCore core = TrueEconomyMod.core();
        if (core.economy().transfer(owner, args[1], amount)) {
            double tax = core.government().levyTransactionTax("default", amount);
            reply(p, "Paid " + core.economy().format(amount) + " to " + args[1] + " (tax: " + core.economy().format(tax) + ")");
        } else {
            error(p, "Insufficient funds.");
        }
        core.save();
    }

    // ---------------- market ----------------
    private static void market(ServerPlayer p, String owner, String[] args) {
        if (args.length < 2) { error(p, "Usage: /economy market <list|view <id>|trade <id> <amount> [sell]>"); return; }
        EconomyCore core = TrueEconomyMod.core();
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "list":
                StringBuilder sb = new StringBuilder("[Market] ");
                for (Commodity c : core.market().all()) {
                    sb.append(c.getName()).append(" ").append(core.economy().format(c.getCurrentPrice())).append("  ");
                }
                reply(p, sb.toString());
                break;
            case "view":
                if (args.length < 3) { error(p, "Usage: /economy market view <id>"); return; }
                Commodity c = core.market().get(args[2]);
                if (c == null) { error(p, "Unknown commodity: " + args[2]); return; }
                reply(p, "[Market] " + c.getName() + " price " + core.economy().format(c.getCurrentPrice())
                        + " S:" + String.format("%.1f", c.getSupply()) + " D:" + String.format("%.1f", c.getDemand()));
                break;
            case "trade":
                if (args.length < 4) { error(p, "Usage: /economy market trade <id> <amount> [sell]"); return; }
                int units = numI(args, 3, 0);
                if (units <= 0) { error(p, "Invalid amount."); return; }
                MarketManager.TradeResult r = args.length > 4 && "sell".equalsIgnoreCase(args[4])
                        ? core.market().sell(core.economy(), owner, args[2], units)
                        : core.market().buy(core.economy(), owner, args[2], units);
                if (r.ok) reply(p, r.message); else error(p, r.message);
                core.save();
                break;
            default:
                error(p, "Unknown market subcommand.");
        }
    }

    // ---------------- auction ----------------
    private static void auction(ServerPlayer p, String owner, String[] args) {
        if (args.length < 2) { error(p, "Usage: /economy auction <list|create <id> <bid> [buynow]|bid <id> <amount>|buy <id>|claim <id>>"); return; }
        EconomyCore core = TrueEconomyMod.core();
        AuctionHouse ah = core.auctions();
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "list":
                List<Auction> acts = ah.getActive();
                if (acts.isEmpty()) { reply(p, "[Auctions] none active."); return; }
                for (Auction a : acts) {
                    reply(p, String.format("[Auctions] %s seller=%s bid=%s%s",
                            a.getItemId(), a.getSeller(),
                            core.economy().format(a.getHighestBid() == 0 ? a.getStartingBid() : a.getHighestBid()),
                            a.getBuyItNowPrice() > 0 ? " buynow=" + core.economy().format(a.getBuyItNowPrice()) : ""));
                }
                break;
            case "create":
                if (args.length < 4) { error(p, "Usage: /economy auction create <id> <bid> [buynow]"); return; }
                double bid = num(args, 3, -1);
                if (bid <= 0) { error(p, "Invalid bid."); return; }
                if (!core.economy().getAccount(owner).withdraw(ah.getListingFee())) {
                    error(p, "Cannot afford the " + core.economy().format(ah.getListingFee()) + " listing fee.");
                    return;
                }
                double buynow = args.length > 4 ? num(args, 4, 0) : 0;
                Auction a = new Auction(owner, args[2], core.config().auctionDefaultDuration * 1000L, bid, buynow);
                ah.add(a);
                reply(p, "[Auctions] Listed " + args[2] + " (id " + a.getId() + "). Fee: " + core.economy().format(ah.getListingFee()));
                core.save();
                break;
            case "bid":
                if (args.length < 4) { error(p, "Usage: /economy auction bid <id> <amount>"); return; }
                Auction target = ah.get(UUID.fromString(args[2]));
                double amt = num(args, 3, -1);
                if (target == null) { error(p, "No such auction."); return; }
                if (amt <= 0 || !ah.placeBid(target, owner, amt)) { error(p, "Bid too low."); return; }
                core.economy().getAccount(owner).withdraw(amt);
                reply(p, "[Auctions] Bid placed: " + core.economy().format(amt));
                core.save();
                break;
            case "buy":
                if (args.length < 3) { error(p, "Usage: /economy auction buy <id>"); return; }
                Auction b = ah.get(UUID.fromString(args[2]));
                if (b == null || b.getBuyItNowPrice() <= 0) { error(p, "Not for sale at buy-it-now."); return; }
                double fee = b.getBuyItNowPrice() * ah.getSalesTax();
                core.economy().getAccount(owner).withdraw(b.getBuyItNowPrice());
                ah.buyNow(b, owner, fee);
                ah.remove(b.getId());
                core.economy().getAccount(b.getSeller()).modifyBalance(b.getHighestBid() - fee);
                reply(p, "[Auctions] Bought " + b.getItemId() + " for " + core.economy().format(b.getBuyItNowPrice()));
                core.save();
                break;
            case "claim":
                if (args.length < 3) { error(p, "Usage: /economy auction claim <id>"); return; }
                Auction c = ah.get(UUID.fromString(args[2]));
                if (c == null || !owner.equals(c.getSeller())) { error(p, "Not your auction."); return; }
                ah.remove(c.getId());
                reply(p, "[Auctions] Claimed " + c.getItemId());
                core.save();
                break;
            default:
                error(p, "Unknown auction subcommand.");
        }
    }

    // ---------------- bank ----------------
    private static void bank(ServerPlayer p, String owner, String[] args) {
        if (args.length < 2) { error(p, "Usage: /economy bank <deposit|withdraw|interest|loan <amt>|credit|repay <amt>>"); return; }
        EconomyCore core = TrueEconomyMod.core();
        BankManager bank = core.bank();
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "deposit": {
                double v = num(args, 2, -1);
                if (v <= 0) { error(p, "Invalid amount."); return; }
                Account acc = core.economy().getAccount(owner);
                if (!acc.depositBank(v)) { error(p, "Insufficient wallet funds."); return; }
                reply(p, "Deposited " + core.economy().format(v) + " (Wallet " + core.economy().format(acc.getBalance()) + ")");
                core.save();
                break;
            }
            case "withdraw": {
                double v = num(args, 2, -1);
                if (v <= 0) { error(p, "Invalid amount."); return; }
                Account acc = core.economy().getAccount(owner);
                if (!acc.withdrawBank(v)) { error(p, "Insufficient bank balance."); return; }
                reply(p, "Withdrew " + core.economy().format(v) + " (Bank " + core.economy().format(acc.getBankBalance()) + ")");
                core.save();
                break;
            }
            case "interest":
                reply(p, "[Bank] Interest " + String.format("%.1f", bank.getInterestRatePerCycle() * 100) + "%/cycle, max balance "
                        + core.economy().format(bank.getMaxAccountBalance()));
                break;
            case "loan": {
                double v = num(args, 2, -1);
                Account acc = core.economy().getAccount(owner);
                if (v <= 0) { error(p, "Usage: /economy bank loan <amount>"); return; }
                if (!bank.isLoanAllowed(acc.getCreditScore())) {
                    error(p, "Credit score too low (need " + bank.getDefaultMinCreditScore() + ")."); return;
                }
                if (v > bank.getMaxLoanAmount()) { error(p, "Exceeds max loan " + core.economy().format(bank.getMaxLoanAmount())); return; }
                Loan loan = bank.createLoan(owner, v, acc.getCreditScore());
                acc.modifyBalance(v);
                reply(p, "[Bank] Loan approved: " + core.economy().format(loan.getRemaining()) + " credited.");
                core.save();
                break;
            }
            case "credit": {
                Account acc = core.economy().getAccount(owner);
                double outstanding = 0;
                for (Loan l : bank.getLoans(owner)) if (l.getStatus() == Loan.Status.ACTIVE) outstanding += l.getRemaining();
                reply(p, "[Bank] Credit score " + acc.getCreditScore() + ", outstanding " + core.economy().format(outstanding));
                break;
            }
            case "repay": {
                double v = num(args, 2, -1);
                if (v <= 0) { error(p, "Usage: /economy bank repay <amount>"); return; }
                Account acc = core.economy().getAccount(owner);
                if (acc.getBalance() < v) { error(p, "Insufficient funds."); return; }
                acc.withdraw(v);
                bank.repay(owner, v);
                reply(p, "[Bank] Repaid " + core.economy().format(v));
                core.save();
                break;
            }
            default:
                error(p, "Unknown bank subcommand.");
        }
    }

    // ---------------- shop / black market ----------------
    private static void shop(ServerPlayer p, String owner, String identity, String[] args) {
        EconomyCore core = TrueEconomyMod.core();
        String canon = core.canonicalIdentity(identity);
        if (args.length < 2 || args[1].equalsIgnoreCase("list")) {
            List<ShopSection> unlocked = core.catalog().unlocked(canon);
            if (unlocked.isEmpty()) { reply(p, "[Shop] Nothing unlocked yet. Discover blocks to open sections."); return; }
            for (ShopSection s : unlocked) {
                reply(p, "[Shop] " + s.getDisplay() + ": " + String.join(", ", s.getItemIds()));
            }
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "hire":
                if (args.length < 3) { error(p, "Usage: /economy shop hire <shop name> <employee>"); return; }
                PlayerShop h = core.shops().get(args[2]);
                if (h == null) { error(p, "No such shop."); return; }
                String emp = args.length > 3 ? args[3] : owner;
                if (!h.hire(emp)) { error(p, "Cannot hire (cap reached or duplicate)."); return; }
                reply(p, "[Shop] Hired " + emp);
                core.save();
                break;
            case "fire":
                if (args.length < 4) { error(p, "Usage: /economy shop fire <shop name> <employee>"); return; }
                PlayerShop f = core.shops().get(args[2]);
                if (f == null) { error(p, "No such shop."); return; }
                f.fire(args[3]);
                reply(p, "[Shop] Fired " + args[3]);
                core.save();
                break;
            case "buy":
                if (args.length < 4) { error(p, "Usage: /economy shop buy <section> <item>"); return; }
                buyBlackMarket(p, core, canon, args[2], args[3]);
                break;
            default:
                error(p, "Unknown shop subcommand.");
        }
    }

    private static void buyBlackMarket(ServerPlayer p, EconomyCore core, String canon, String sectionId, String itemId) {
        for (ShopSection s : core.catalog().all()) {
            if (!s.getId().equalsIgnoreCase(sectionId)) continue;
            if (!s.isUnlocked(core.catalog().discovered(canon))) { error(p, "Section locked."); return; }
            if (!s.getItemIds().stream().anyMatch(i -> i.equalsIgnoreCase(itemId))) { error(p, "Item not in section."); return; }
            Commodity c = core.market().get(itemId);
            if (c == null) { error(p, "No price for " + itemId); return; }
            double price = c.withModifier(core.seasonal().modifierFor(c.getId()));
            double total = price * 1.5;
            if (!core.economy().getAccount(p.getGameProfile().name()).withdraw(total)) { error(p, "Insufficient funds."); return; }
            core.economy().getAccount(core.market().getMarketAccount()).deposit(total);
            reply(p, "[Black Market] Bought 1 " + c.getName() + " for " + core.economy().format(total));
            core.save();
            return;
        }
        error(p, "Unknown section " + sectionId);
    }

    // ---------------- events ----------------
    private static void events(ServerPlayer p, String[] args) {
        EconomyCore core = TrueEconomyMod.core();
        SeasonalEventManager seasonal = core.seasonal();
        List<SeasonalEventManager.MarketEvent> active = seasonal.getActiveEvents();
        if (args.length > 1 && args[1].equalsIgnoreCase("history")) {
            if (seasonal.getHistory().isEmpty()) { reply(p, "[Events] None yet."); return; }
            for (SeasonalEventManager.MarketEvent e : seasonal.getHistory()) {
                reply(p, "[Events] " + (e.isActive() ? "[active] " : "") + e.type + " " + e.commodityId + " x" + e.multiplier);
            }
            return;
        }
        if (active.isEmpty()) {
            long s = Math.max(0, (seasonal.getNextEventTime() - System.currentTimeMillis()) / 1000);
            reply(p, "[Events] No active market events. Next in ~" + s + "s.");
        } else {
            for (SeasonalEventManager.MarketEvent e : active) reply(p, "[Events] " + e.type + " " + e.commodityId + " x" + e.multiplier);
        }
    }

    // ---------------- link ----------------
    private static void link(ServerPlayer p, String owner, String identity, String[] args) {
        LinkManager links = TrueEconomyMod.core().links();
        if (args.length < 2) { error(p, "Usage: /economy link start | /economy link <code>"); return; }
        if (args[1].equalsIgnoreCase("start")) {
            String code = links.startLink("uuid:" + p.getUUID(), owner);
            reply(p, "[Link] Code: " + code + ". Sign in on your other account and run /economy link " + code + " (expires in 5 min).");
            return;
        }
        LinkManager.Result r = links.completeLink(args[1], "uuid:" + p.getUUID(), owner);
        switch (r) {
            case LINKED: reply(p, "[Link] Identities linked! Unlocks, currency and bank balances merged."); TrueEconomyMod.core().save(); break;
            case NO_SUCH_CODE: error(p, "No pending link with that code."); break;
            case EXPIRED: error(p, "That link code has expired."); break;
            case ALREADY_LINKED: error(p, "One of those identities is already linked."); break;
            case SELF_LINK: error(p, "You cannot link an identity to itself."); break;
            default: error(p, "Link failed.");
        }
    }
}