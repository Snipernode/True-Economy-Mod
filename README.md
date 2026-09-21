# True-Economy (Fabric)

A Fabric rewrite of the True-Economy Spigot plugin, built for **every published Minecraft release from 1.16.5 to 26.3** (32 versions).

Dynamic supply/demand economy with a black market, player shops, bank & loans, auctions, seasonal market events, and cross-account linking — all configured through a single JSON file. No server-side inventory management, no per-item schematics: trading is command-driven (chat UI v1) and every price reacts to supply, demand and events in real time.

## Features

- **Market** — each commodity has supply/demand that shifts price every tick cycle; buy cheap, sell high.
- **Black Market** — shop sections unlock by *discovering* blocks (walk over new block types to add them to the catalog); restricted items cost 1.5x.
- **Shops** — hire up to 5 employees; they generate revenue every cycle while paid a wage.
- **Bank** — deposits earn interest, loans are gated by credit score, repayment tracked.
- **Auctions** — fixed or buy-it-now listings, minimum-bid increments, sales tax.
- **Government** — optional economy rules: transaction tax on trades.
- **Seasonal events** — periodic supply/demand shocks (shortage, surplus, boom, crash).
- **City wallet** — government treasury that market tasks and taxes flow into.
- **Linking** — merge two identities (e.g. alt account) with a 5-minute link code.
- **Data** — everything persists to `config/true-economy/data.json` (Gson), rewritten atomically on change; tick-based upkeep runs on every `END_SERVER_TICK`.

## Quick start

1. Pick a release for your exact Minecraft version, then choose the artifact for your setup:
   - `true-economy-<version>.jar` — universal, loads on both client and dedicated server.
   - `true-economy-client-<version>.jar` — client-only; installs on a player's client for singleplayer/LAN.
   - `true-economy-server-<version>.jar` — dedicated-server-only.
   - `...-quilt.jar` — identical jar, labelled for the **Quilt loader** (Quilt loads Fabric mods as-is).
2. Drop the jar into the `mods/` folder.
3. Add **Fabric API** and **Fabric Loader** for your version on Fabric; on Quilt only Fabric API is needed (Quilt ships its own loader).
4. Start the server (or the client for singleplayer). On first run a default `config/true-economy.json` is written.
5. Run `/economy` for usage.

## Commands

All interaction lives under one root command, parsed as string arguments — stable on every version:

```
/economy balance
/economy pay <player> <amount>
/economy market list
/economy market view <id>
/economy market trade <id> <amount> [sell]
/economy auction list|create <id> <bid> [buynow]|bid <id> <amount>|buy <id>|claim <id>
/economy bank deposit|withdraw|interest|loan <amt>|credit|repay <amt>
/economy shop list
/economy shop hire <shop> [employee]
/economy shop fire <shop> <employee>
/economy shop buy <section> <item>
/economy events [history]
/economy link start
/economy link <code>
```

## Configuration

`config/true-economy.json` (auto-created):

```jsonc
{
  "dataFile": "true-economy/data.json",
  "currencyFormat": "$0.00",
  "startBalance": 100.0,
  "maxBalance": 1000000.0,
  "tickIntervalSeconds": 5.0,
  "marketTicksPerCycle": 20,
  "governmentEnabled": true,
  "transactionTaxRate": 0.05,
  "auctionDefaultDuration": 60,
  "bankInterestRatePerCycle": 0.002,
  "bankMaxAccountBalance": 500000.0,
  "bankMaxLoanAmount": 50000.0,
  "bankDefaultMinCreditScore": 60,
  "playersCanRunShops": true,
  "shopHiringCap": 5,
  "marketAccount": "market",
  "items": [ /* commodity definitions */ ],
  "sections": [ /* discoverable shop sections */ ],
  "merchants": [ /* AI NPC merchants */ ]
}
```

## Building

Requires Java 25 (Minecraft 26.x mandates it) and Gradle 9.

```bash
# all 32 versions
gradle build

# just one version
gradle :versions:1_21_4:build
gradle :versions:26_3:build
```

Output jars: `versions/<version>/build/libs/`.

- **1.16.5 – 1.21.11** use Fabric's classic `fabric-loom` with Yarn mappings; the shipped jar is remapped to intermediary.
- **26.1 – 26.3** are unobfuscated Minecraft: they use `net.fabricmc.fabric-loom` with **no mappings**, Mojang-named glue, and no remapping step.
- **Client/server split** is a metadata switch: each release ships a universal (`environment: "*"`), client, and server jar, and all of them run on Fabric or Quilt.
- Shared logic lives in `core/` (pure Java, no Minecraft imports) and `glue/` (Yarn) / `glue-mojmap/` (Mojang) thin adapters. Per-version shims live under `versions/<ver>/src/main/java` for the few APIs that moved across releases (command registration v1/v2, `World`/`GameProfile` accessors, block registry access).

## Parity notes

The command surface covers every present feature of the original Spigot plugin: market, black market, shops/hiring, bank/loans/credit, auctions, government taxes, seasonal events, city wallet, NPC merchants and identity linking. Inventory-based trading (placing chests with items) and NPC merchants acting as live shopkeepers are not implemented — trading is purely currency-based via commands.