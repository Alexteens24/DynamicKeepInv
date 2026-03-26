# Installation

## Requirements

| Requirement | Version |
|-------------|----------|
| Minecraft Server | 1.20.4 or higher |
| Server Software | Paper, Spigot, Purpur, or Folia |
| Java | 21 or higher |

## Optional Plugins

| Plugin | Purpose |
|--------|----------|
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | Economy integration (pay to keep items) |
| [Lands](https://www.spigotmc.org/resources/lands.53313/) | Per-land keep inventory rules |
| [GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/) | Per-claim keep inventory rules |
| [WorldGuard](https://enginehub.org/worldguard/) | Per-region keep inventory rules |
| [Towny](https://github.com/TownyAdvanced/Towny) | Per-town keep inventory rules |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Placeholder support |
| GravesX / AxGraves | Grave creation for dropped deaths |
| MMOItems | Protected item tag support |

---

## Installation Steps

### Step 1: Download

Get the latest JAR from:
- [GitHub Releases](https://github.com/Alexteens24/DynamicKeepInv/releases)
- [Modrinth](https://modrinth.com/plugin/dynamickeepinv)

### Step 2: Install

1. Stop your server
2. Place `DynamicKeepInv-X.X.X.jar` in your `plugins/` folder
3. Start your server

### Step 3: Configure

Edit the generated config files:
```
plugins/DynamicKeepInv/
├── config.yml      # Main settings
└── messages.yml    # Customize messages
```

### Step 4: Verify

Run `/dki status` in-game or console to verify installation.

For deeper diagnostics, run `/dki test <player>` to see which rule would currently apply.

---

## Updating

1. Stop server
2. Replace old JAR with new JAR
3. Start server
4. Run `/dki reload` if config format changed

> **Note:** Config migrations are automatic. Your settings will be preserved.

---

## Troubleshooting

### Plugin not loading

1. Check console for errors
2. Verify Java version: `java -version` (must be 21+)
3. Verify server version (must be 1.20.4+)

### Protection plugin not detected

1. Make sure they load before DynamicKeepInv
2. Check console for the hook message
3. Enable the correct config path, for example:
	- `integrations.lands.enabled: true`
	- `integrations.griefprevention.enabled: true`
	- `integrations.worldguard.enabled: true`
	- `integrations.towny.enabled: true`

### Economy not working

1. Install Vault
2. Install an economy plugin (EssentialsX, CMI, etc.)
3. Set `economy.enabled: true` in config

---

## Next Steps

- [Basic Configuration](Basic-Configuration) - Set up time and world settings
- [Advanced Configuration](Advanced-Configuration) - Death cause, protection plugins, economy
