# DynamicKeepInv

Automatically toggles keepInventory gamerule based on Minecraft day/night cycle.

## Features

- Automatic keep inventory during day, disabled at night
- Supports Paper/Spigot 1.20+ and Folia
- Per-world configuration
- Customizable time thresholds
- Broadcast notifications
- Multi-language support (English/Vietnamese)

## Installation

1. Download the plugin JAR file
2. Place it in your server's `plugins/` folder
3. Restart the server
4. Configure `plugins/DynamicKeepInv/config.yml` as needed

## Configuration

**config.yml:**
```yaml
enabled: true
keep-inventory-day: true
keep-inventory-night: false
check-interval: 100
day-start: 0
night-start: 13000
enabled-worlds: []
debug: false
```

**messages.yml:**
```yaml
language: en  # or 'vi' for Vietnamese
```

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/dki status` | Show plugin status | `dynamickeepinv.admin` |
| `/dki reload` | Reload configuration | `dynamickeepinv.admin` |
| `/dki enable` | Enable plugin | `dynamickeepinv.admin` |
| `/dki disable` | Disable plugin | `dynamickeepinv.admin` |
| `/dki toggle` | Toggle plugin on/off | `dynamickeepinv.admin` |

Aliases: `/dki`, `/keepinv`, `/dynamickeepinv`

## Building

```bash
mvn clean package
```

Output: `target/DynamicKeepInv-1.0.0.jar`

## License

This project is licensed under the MIT License.
