# DynamicKeepInv

A lightweight Minecraft plugin that dynamically controls the keepInventory gamerule based on in-game time, protection plugins, and death causes.

## Features

- **Time-based Control** - Keep inventory during day, drop items at night
- **Protection Plugin Integration** - Lands and GriefPrevention support with per-area settings
- **Death Cause Rules** - Different settings for PvP vs PvE deaths
- **Economy Support** - Charge players to keep inventory (requires Vault)
- **Per-world Settings** - Configure each world independently
- **Folia Compatible** - Full support for Paper, Spigot, and Folia servers
- **Minimal Performance Impact** - Efficient tick-based checking

## Requirements

- Minecraft 1.20.4+
- Java 17+
- Paper/Spigot/Folia server

## Quick Start

1. Download the latest release
2. Place JAR in `plugins/` folder
3. Restart server
4. Edit `config.yml` to customize

## Documentation

Full documentation available on the [Wiki](https://github.com/Alexteens24/DynamicKeepInv/wiki).

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/dki status` | Show current status | `dynamickeepinv.admin` |
| `/dki reload` | Reload configuration | `dynamickeepinv.admin` |
| `/dki toggle` | Toggle plugin on/off | `dynamickeepinv.admin` |

## Permissions

| Permission | Description |
|------------|-------------|
| `dynamickeepinv.admin` | Access to all commands |
| `dynamickeepinv.bypass` | Always keep inventory |

## Building

```bash
mvn clean package
```

## License

Apache License 2.0
