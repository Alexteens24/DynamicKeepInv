# Commands

All commands use the base `/dynamickeepinv` or the alias `/dki`.

## Command List

| Command | Description | Permission |
|---------|-------------|------------|
| `/dki` | Show help | - |
| `/dki help` | Show help | - |
| `/dki status` | Show plugin status | `dynamickeepinv.status` |
| `/dki reload` | Reload configuration | `dynamickeepinv.reload` |
| `/dki enable` | Enable the plugin | `dynamickeepinv.toggle` |
| `/dki disable` | Disable the plugin | `dynamickeepinv.toggle` |
| `/dki toggle` | Toggle on/off | `dynamickeepinv.toggle` |

## Status Command

Shows current plugin status including:
- Plugin enabled/disabled
- Current time in each world
- Keep inventory status for each world
- Active integrations (Lands, GP, Economy)

Example output:
```
=== DynamicKeepInv Status ===
Plugin: Enabled
World 'world': Day (tick 6000) - Keep Inventory: ON
World 'world_nether': - Keep Inventory: OFF
Lands: Enabled
Economy: Disabled
```

## Reload Command

Reloads `config.yml` and `messages.yml` without restarting the server.

**Note**: Some changes (like Lands/GP integration) may require a full server restart.
