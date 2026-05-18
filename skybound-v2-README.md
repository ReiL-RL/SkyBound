# SkyBound v2

Modular SkyBlock plugin with **Core + Addon** architecture.

## Architecture

```
skybound-api    → Public interfaces, models, events (jar shared by all)
skybound-core   → Main plugin: islands, economy, teams, upgrades, generators, missions, shop, bank, boosters, leaderboard
```

Future addons (separate plugins that also work standalone):
- `skybound-events` — Event system, portals, zones, wave spawner
- `skybound-elitemobs` — EliteMobs integration bridge
- `skybound-achievements` — Achievement/challenge system
- `skybound-dungeons` — Dungeon instances

## Features (Core)

Inspired by [IridiumSkyblock](https://github.com/Iridium-Development/IridiumSkyblock), SuperiorSkyblock2, and BentoBox:

| Feature | Description |
|---------|-------------|
| **Dimensional Islands** | Overworld, Nether, End — unlock by level |
| **Ore Generator** | Tiered cobblestone generator with configurable ore distribution |
| **Island Bank** | Shared team bank with money + crystals |
| **Upgrades** | Permanent island improvements (size, team, generator, hoppers, crops, spawners) |
| **Boosters** | Temporary island buffs (farming, XP, spawner, generator, flight) |
| **Missions** | Tiered missions with auto-tracking (break, place, kill, craft, fish, etc.) |
| **Shop** | Category-based buy/sell with command items |
| **Leaderboard** | Top islands by value or level |
| **Teams** | Invite, kick, promote, demote, trust, co-op, transfer ownership |
| **Roles & Permissions** | 7-tier role system with granular permissions |
| **Island Warps** | Player-set warps visible to visitors |
| **Island Lock** | Toggle visitor access |
| **Regeneration** | Reset island with new schematic |
| **Spiral Grid** | Efficient island placement algorithm |
| **Addon System** | Register addons via API, dual-mode (addon or standalone) |

## API

Addons depend on `skybound-api` (provided scope) and access services through:

```java
SkyBoundAPI api = SkyBoundAPI.get();
IslandProvider islands = api.getIslandProvider();
EconomyProvider economy = api.getEconomyProvider();
```

Custom Bukkit events for inter-addon communication:
- `IslandCreateEvent`, `IslandDeleteEvent`
- `IslandMemberJoinEvent`, `IslandMemberLeaveEvent`
- `IslandLevelUpEvent`, `IslandUpgradeEvent`
- `GeneratorProduceEvent`, `MissionCompleteEvent`

## Build

```bash
mvn clean package
```

Output: `skybound-core/target/skybound-core-2.0.0-SNAPSHOT.jar`

## Requirements

- Spigot/Paper 1.16.5+
- Java 8+
- Vault (required for economy)
- WorldEdit (optional, for schematics)

## Target: Spigot 1.16.5, Java 8
