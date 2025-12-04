# Commands

Base command: `/dynamickeepinv`  
Aliases: `/dki`, `/keepinv`

---

## Command Reference

| Command | Description | Permission |
|---------|-------------|------------|
| `/dki` | Show help menu | None |
| `/dki help` | Show help menu | None |
| `/dki status` | Show plugin status | `dynamickeepinv.status` |
| `/dki reload` | Reload config files | `dynamickeepinv.reload` |
| `/dki enable` | Enable the plugin | `dynamickeepinv.toggle` |
| `/dki disable` | Disable the plugin | `dynamickeepinv.toggle` |
| `/dki toggle` | Toggle plugin on/off | `dynamickeepinv.toggle` |

---

## Command Details

### `/dki status`

Shows comprehensive plugin status:

```
=== DynamicKeepInv Status ===
Plugin: Enabled
World 'world': Day (tick 6000) - Keep Inventory: ON
World 'world_nether': Night (tick 18000) - Keep Inventory: OFF
World 'world_the_end': Keep Inventory: ON

Integrations:
  Lands: Enabled
  GriefPrevention: Disabled
  Economy: Disabled
```

Information displayed:
- Plugin enabled/disabled state
- Each world's current time (tick)
- Current keep inventory status per world
- Active plugin integrations

### `/dki reload`

Reloads configuration without server restart:
- `config.yml` - All settings
- `messages.yml` - All messages

**What gets reloaded:**
- Time settings
- World settings
- Broadcast settings
- Economy settings
- Protection plugin settings

**What requires full restart:**
- Initial Lands/GriefPrevention hook (if plugin wasn't detected on startup)

### `/dki enable` / `/dki disable` / `/dki toggle`

Control the plugin without editing config:

```
/dki disable  → Plugin paused, keepInventory won't change
/dki enable   → Plugin resumed
/dki toggle   → Flip current state
```

> **Note:** This is temporary. Plugin state resets to config value on restart.

---

## Console Commands

All commands work from console without the `/` prefix:

```
dki status
dki reload
```

---

## Related

- [Permissions](Permissions) - Permission nodes for commands
