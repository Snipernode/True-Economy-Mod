# True-Economy

A multi-loader rewrite of the True-Economy Spigot plugin, built for **every published Minecraft release from 1.16.5 to 26.3**.

Available for:
- **Fabric / Quilt** — 32 versions (1.16.5 → 26.3).
- **Forge** — 9 versions (1.17.1, 1.18.2, 1.19, 1.19.1, 1.19.2, 1.19.3, 1.19.4, 1.20, 1.20.1). One universal jar per version.
- **NeoForge** — 17 versions (1.20.4, 1.20.6, 1.21–1.21.11, 26.1, 26.1.1, 26.1.2). One universal jar per version.

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

### Fabric / Quilt

1. Pick a release for your exact Minecraft version, then choose the artifact for your setup:
   - `true-economy-<version>.jar` — universal, loads on both client and dedicated server.
   - `true-economy-client-<version>.jar` — client-only; installs on a player's client for singleplayer/LAN.
   - `true-economy-server-<version>.jar` — dedicated-server-only.
   - `...-quilt.jar` — identical jar, labelled for the **Quilt loader** (Quilt loads Fabric mods as-is).
2. Drop the jar into the `mods/` folder.
3. Add **Fabric API** and **Fabric Loader** for your version on Fabric; on Quilt only Fabric API is needed (Quilt ships its own loader).
4. Start the server (or the client for singleplayer). On first run a default `config/true-economy.json` is written.
5. Run `/economy` for usage.

### Forge / NeoForge

1. Pick the release for your exact Minecraft version; the Forge jar is `true-economy-forge-<version>.jar` and the NeoForge jar is `true-economy-neoforge-<version>.jar` (single universal jar per version for these loaders).
2. Drop the jar into the `mods/` folder **and** install the matching loader build (Forge for 1.17.1–1.20.1, NeoForge for 1.20.4+).
3. Start the server. On first run a default `config/true-economy.json` is written. `/economy` shows usage.

> Not shipped on these loaders: **Forge 1.16.5** (classic ForgeGradle-only; the entire 1.16.5 range is covered on Fabric/Quilt instead), **NeoForge 1.20.2/1.20.3/1.20.5** (no `-moddev-bundle` published for these short-lived lines), and **NeoForge 26.2/26.3** (upstream NeoForm cannot yet recompile these unobfuscated versions' Java 25 preview sources).

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

The matrix needs two toolchains:

- **Fabric (all 32):** Java 25 (Minecraft 26.x mandates it) and Gradle 9. `java-home=/tmp/opencode/tool/jdk25`, `/tmp/opencode/tool/gradle9/gradle-9.5.0/bin/gradle`.
- **Forge/NeoForge:** Gradle 8.14.3 running on JDK 21, toolchains resolved via foojay (Java 17 for 1.20.x, 21 for 1.21.x, 25/26 for 26.x). `java-home=/tmp/opencode/tool/jdk21`, `/tmp/opencode/tool/gradle8/gradle-8.14.3/bin/gradle`. NeoForge goes through ModDevGradle (`net.neoforged.moddev`), Forge 1.17.1–1.20.1 through its legacy Forge wrapper.

```bash
# all 32 fabric versions
gradle build

# just one version
gradle :versions:1_21_4:build
gradle :versions:26_3:build

# a loader version
gradle :neoforge:1_21_4:build
gradle :forge:1_20_1:build
```

Output jars: `<subproject>/build/libs/`.

- **Fabric 1.16.5 – 1.21.11** use Fabric's classic `fabric-loom` with Yarn mappings; the shipped jar is remapped to intermediary. **26.1 – 26.3** are unobfuscated Minecraft: they use `net.fabricmc.fabric-loom` with **no mappings**, Mojang-named glue, and no remapping step.
- **Client/server split** is a fabric-metadata switch: each release ships a universal (`environment: "*"`), client, and server jar, all of which run on Fabric or Quilt.
- **Forge/NeoForge** ship a single universal jar per version (loader metadata has no environment gating). Minecraft versions map to loaders as: Forge ≤ 1.20.1, NeoForge ≥ 1.20.2 (1.20.4 and 1.20.6 onwards buildable via ModDevGradle).
- Shared logic lives in `core/` (pure Java, no Minecraft imports) and `glue/` (Yarn) / `glue-mojmap/` (Mojang) / `glue-forge/` (Forge+NeoForge shared) thin adapters. Per-version shims live under each subproject's `src/main/java` for the APIs that moved across releases (command registration v1/v2, tick events, `World`/`GameProfile`/level accessors, block registry access, chat-message API).

## Parity notes

The command surface covers every present feature of the original Spigot plugin: market, black market, shops/hiring, bank/loans/credit, auctions, government taxes, seasonal events, city wallet, NPC merchants and identity linking. Inventory-based trading (placing chests with items) and NPC merchants acting as live shopkeepers are not implemented — trading is purely currency-based via commands.