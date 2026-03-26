# DynamicKeepInv

DynamicKeepInv gives you rule-based control over `keepInventory` instead of a single global on/off switch.

## What It Can Do

- Keep items during the day and drop them at night
- Override day/night with PvP vs PvE logic
- Apply special rules inside Lands, GriefPrevention, WorldGuard, or Towny areas
- Let players pay to keep items with an optional death GUI
- Protect first deaths or repeated deaths in a short streak
- Send items to GravesX / AxGraves instead of dropping them on the ground
- Track stats and expose them through PlaceholderAPI
- Help admins debug decisions with `/dki test [player]`

## Current Rule Order

1. Bypass permission
2. First death rule
3. Death streak rule
4. Protection integrations
5. Death cause rule
6. Day/night fallback

## Quick Start

1. Install Java 21 and a 1.20.4+ Paper/Spigot/Purpur/Folia server.
2. Put the plugin JAR into `plugins/`.
3. Start the server once to generate config files.
4. Edit `plugins/DynamicKeepInv/config.yml`.
5. Run `/dki reload`.

## Good Defaults

Out of the box:

- Day: keep items and XP
- Night: lose items and XP
- Stats: enabled
- Optional integrations: disabled until enabled in config

## Pages

| Page | Description |
|------|-------------|
| [Installation](Installation) | Requirements, setup, updating, troubleshooting |
| [Basic Configuration](Basic-Configuration) | Core settings, time, worlds, broadcasts |
| [Advanced Configuration](Advanced-Configuration) | Death-cause rules, economy, integrations, new rules |
| [Commands](Commands) | All player and admin commands |
| [Permissions](Permissions) | Permission nodes and recommended setups |
| [Stats](Stats) | Player stats GUI and tracked values |
| [Placeholders](Placeholders) | PlaceholderAPI output list |
| [FAQ](FAQ) | Common questions |

## Support

- [GitHub Issues](https://github.com/Alexteens24/DynamicKeepInv/issues)
- [Modrinth](https://modrinth.com/plugin/dynamickeepinv)
