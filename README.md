# DynamicKeepInv

![Java](https://img.shields.io/badge/Java-21%2B-orange) ![Server](https://img.shields.io/badge/Server-Paper%20%7C%20Spigot%20%7C%20Folia-blue) ![License](https://img.shields.io/badge/License-Apache%202.0-green)

DynamicKeepInv makes `keepInventory` configurable instead of binary. You can decide whether players keep items and XP based on time of day, death cause, protection plugins, economy rules, and optional safety-net rules.

Built for 1.20.4+ servers with Java 21 and full Folia support.

---

## Highlights

- Day/night keep-inventory rules with separate item and XP control
- PvP vs PvE overrides
- Per-world enable list and per-world item overrides
- Lands, GriefPrevention, WorldGuard, and Towny integration
- Economy support with `charge-to-keep`, `charge-to-bypass`, or `gui`
- Death confirmation GUI and player auto-pay toggle
- First-death leniency and death-streak protection rules
- Grave integration with GravesX / AxGraves and fallback handling
- Player stats GUI plus PlaceholderAPI support
- Admin diagnostics with `/dki status` and `/dki test [player]`

---

## Rule Order

When a player dies, the plugin evaluates rules in this order:

1. `BypassPermissionRule`
2. `FirstDeathRule` if enabled
3. `DeathStreakRule` if enabled
4. `ProtectionRule` for Lands / GriefPrevention / WorldGuard / Towny
5. `DeathCauseRule` for PvP vs PvE
6. `WorldTimeRule` for day/night fallback

The first rule that returns a result wins.

---

## Requirements

- Java 21+
- Minecraft server 1.20.4+
- Paper, Spigot, Purpur, or Folia

Optional plugins:

- Vault for economy features
- PlaceholderAPI for placeholders
- Lands / GriefPrevention / WorldGuard / Towny for protection-aware rules
- GravesX / AxGraves for graves instead of ground drops
- MMOItems for protected tag handling

---

## Installation

1. Download the latest JAR from [Releases](https://github.com/Alexteens24/DynamicKeepInv/releases).
2. Place it in your server's `plugins/` folder.
3. Install any optional integrations you want.
4. Start the server.
5. Edit `plugins/DynamicKeepInv/config.yml`.
6. Run `/dki reload` after config changes.

Config migration runs automatically on startup and reload.

---

## Commands

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/dki status` | `dynamickeepinv.admin` | Show current plugin state, world state, and active rule chain |
| `/dki reload` | `dynamickeepinv.admin` | Reload config, messages, integrations, GUI, and stats systems |
| `/dki enable` | `dynamickeepinv.admin` | Enable the plugin globally |
| `/dki disable` | `dynamickeepinv.admin` | Disable the plugin globally |
| `/dki toggle` | `dynamickeepinv.admin` | Toggle global enabled state |
| `/dki test [player]` | `dynamickeepinv.admin` | Diagnose which rule would apply to a player |
| `/dki stats [player]` | `dynamickeepinv.stats` / `dynamickeepinv.stats.others` | Open stats GUI |
| `/dki confirm` | `dynamickeepinv.use` | Reopen pending death GUI in `gui` mode |
| `/dki autopay` | `dynamickeepinv.use` | Toggle auto-pay in `gui` mode |

Useful permissions:

- `dynamickeepinv.bypass`
- `dynamickeepinv.stats.others`

---

## Config Example

```yaml
rules:
  day:
    keep-items: true
    keep-xp: true
  night:
    keep-items: false
    keep-xp: false
  death-cause:
    enabled: true
    pvp:
      keep-items: false
      keep-xp: false
    pve:
      keep-items: true
      keep-xp: true
  first-death:
    enabled: true
    keep-items: true
    keep-xp: true
  streak:
    enabled: true
    threshold: 3
    window-seconds: 300
    keep-items: false
    keep-xp: false

integrations:
  worldguard:
    enabled: true
    in-own-region:
      keep-items: true
      keep-xp: true
    in-other-region:
      keep-items: false
      keep-xp: false
  towny:
    enabled: true
    in-own-town:
      keep-items: true
      keep-xp: true
    in-other-town:
      keep-items: false
      keep-xp: false

economy:
  enabled: true
  cost: 100.0
  mode: "gui"
  gui:
    timeout: 30
    expire-time: 300
```

---

## Placeholders

Requires PlaceholderAPI.

- `%dynamickeepinv_enabled%`
- `%dynamickeepinv_keepinventory_formatted%`
- `%dynamickeepinv_period%`
- `%dynamickeepinv_economy_cost%`
- `%dynamickeepinv_stats_total_deaths%`
- `%dynamickeepinv_stats_save_rate%`

See the wiki pages for full details.

---

## Wiki

- [Home](wiki/Home.md)
- [Installation](wiki/Installation.md)
- [Basic Configuration](wiki/Basic-Configuration.md)
- [Advanced Configuration](wiki/Advanced-Configuration.md)
- [Commands](wiki/Commands.md)
- [Permissions](wiki/Permissions.md)
- [Stats](wiki/Stats.md)
- [Placeholders](wiki/Placeholders.md)

---

## License

This project is licensed under the [Apache 2.0 License](LICENSE).
