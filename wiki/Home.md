# DynamicKeepInv

A lightweight Minecraft plugin that dynamically controls the `keepInventory` gamerule based on in-game time, protection plugins, and death causes.

## Why This Plugin?

Vanilla Minecraft only has a simple on/off for keepInventory. This plugin gives you granular control:

- Keep items during day, lose them at night
- Different rules for PvP vs PvE deaths
- Integration with land claim plugins
- Per-world settings
- Economy support (pay to keep items)

## Features

| Feature | Description |
|---------|-------------|
| **Time-based** | Different settings for day and night |
| **Death cause** | Separate rules for PvP and PvE |
| **Protection plugins** | Lands & GriefPrevention integration |
| **Per-world** | Configure each world independently |
| **Economy** | Charge players to keep inventory |
| **Folia support** | Works on Paper, Spigot, and Folia |

## Quick Start

1. Download the [latest release](https://github.com/Alexteens24/DynamicKeepInv/releases)
2. Place JAR in your `plugins/` folder
3. Restart the server
4. Edit `plugins/DynamicKeepInv/config.yml`
5. Run `/dki reload`

## Default Behavior

Out of the box:
- **Day (0-12999 ticks)**: Players keep inventory
- **Night (13000-23999 ticks)**: Players lose inventory

## Pages

| Page | Description |
|------|-------------|
| [Installation](Installation) | Requirements and setup |
| [Basic Configuration](Basic-Configuration) | Time, world, and broadcast settings |
| [Advanced Configuration](Advanced-Configuration) | Death cause, protection plugins, economy |
| [Commands](Commands) | All available commands |
| [Permissions](Permissions) | Permission nodes |
| [FAQ](FAQ) | Common questions and examples |

## Support

- [GitHub Issues](https://github.com/Alexteens24/DynamicKeepInv/issues) - Bug reports and feature requests
- [Modrinth](https://modrinth.com/plugin/dynamickeepinv) - Downloads and reviews
