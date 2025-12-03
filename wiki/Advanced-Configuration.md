# Advanced Configuration

⚠️ **These settings are for experienced users.** Make sure you understand how they work before enabling.

## Enable Advanced Features

```yaml
advanced:
  enabled: true  # Must be true to use any advanced features
```

## Priority Order

When multiple settings apply, this is the priority (highest first):

1. **Bypass permission** - `dynamickeepinv.bypass` always keeps inventory
2. **Claimed areas** - in-own-land, in-other-land (if using Lands/GP)
3. **Death cause** - PvP or PvE rules
4. **Wilderness** - Outside any claimed area
5. **Time-based** - Day/night settings (lowest priority)

---

## Bypass Permission

```yaml
advanced:
  bypass-permission: true
```

Players with `dynamickeepinv.bypass` will **always keep inventory**, regardless of any other settings.

---

## Lands Plugin Integration

```yaml
advanced:
  protection:
    lands:
      enabled: true
      override-lands: false  # true = override Lands' own keep inventory
      
      in-own-land:           # Your land or land you're trusted in
        keep-items: true
        keep-xp: true
      
      in-other-land:         # Someone else's land
        keep-items: true
        keep-xp: true
      
      wilderness:            # Outside any land
        enabled: true
        keep-items: false
        keep-xp: false
```

### Notes:
- Lands plugin has its own keep inventory feature
- Set `override-lands: true` only if you want this plugin to control instead
- Wilderness settings can be overridden by death-cause settings

---

## GriefPrevention Integration

```yaml
advanced:
  protection:
    griefprevention:
      enabled: true
      
      in-own-claim:
        keep-items: true
        keep-xp: true
      
      in-other-claim:
        keep-items: false
        keep-xp: false
      
      wilderness:
        enabled: true
        keep-items: false
        keep-xp: false
```

---

## Death Cause Settings

Different rules for PvP vs PvE deaths:

```yaml
advanced:
  death-cause:
    enabled: true
    
    pvp:                    # Killed by another player
      keep-items: true
      keep-xp: true
    
    pve:                    # Killed by mob, fall, lava, etc.
      keep-items: false
      keep-xp: true
```

### Examples:

| Scenario | Config | Result |
|----------|--------|--------|
| PvP in wilderness | `wilderness.keep-items: false`, `pvp.keep-items: true` | **KEEP** (death-cause overrides) |
| PvE in wilderness | `wilderness.keep-items: false`, `pve.keep-items: false` | **DROP** |
| PvP in own land | `in-own-land.keep-items: true` | **KEEP** (claimed area has priority) |

---

## Economy Settings

Requires [Vault](https://www.spigotmc.org/resources/vault.34315/) plugin.

```yaml
advanced:
  economy:
    enabled: true
    cost: 100.0
    mode: "charge-to-keep"  # or "charge-to-bypass"
```

### Modes:

| Mode | Description |
|------|-------------|
| `charge-to-keep` | Pay when you WOULD keep items (pay for the service) |
| `charge-to-bypass` | Pay to KEEP items when you would normally LOSE them |

---

## Death Message

Notify player about their inventory status when they die:

```yaml
advanced:
  death-message:
    enabled: true
    chat: true
    action-bar: false
```

---

## Time-based Item/XP Control

Separate control for items and XP:

```yaml
advanced:
  day:
    keep-items: true
    keep-xp: true
  
  night:
    keep-items: false
    keep-xp: true   # Keep XP even at night
```

---

## Complete Example

Here's a complete advanced config for a typical survival server:

```yaml
advanced:
  enabled: true
  bypass-permission: true
  
  protection:
    lands:
      enabled: true
      override-lands: false
      in-own-land:
        keep-items: true
        keep-xp: true
      in-other-land:
        keep-items: true
        keep-xp: true
      wilderness:
        enabled: true
        keep-items: false
        keep-xp: true
  
  death-cause:
    enabled: true
    pvp:
      keep-items: true    # No penalty for PvP
      keep-xp: true
    pve:
      keep-items: false   # Drop items when killed by mobs
      keep-xp: true       # But keep XP
  
  economy:
    enabled: false
  
  death-message:
    enabled: true
    chat: true
    action-bar: false
```

With this config:
- In any land: Keep everything
- In wilderness + PvP: Keep everything
- In wilderness + PvE: Drop items, keep XP
- Admins with bypass: Always keep everything
