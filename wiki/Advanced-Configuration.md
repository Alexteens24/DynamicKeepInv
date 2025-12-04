# Advanced Configuration

These settings give you fine-grained control over keep inventory behavior.

> ⚠️ **Note:** You must set `advanced.enabled: true` to use any of these features.

---

## How It Works

When a player dies, the plugin checks settings in this order:

```
1. Bypass Permission     → Always keep (if player has permission)
      ↓
2. Claimed Area         → in-own-land / in-other-land settings
      ↓
3. Death Cause          → PvP or PvE settings
      ↓
4. Wilderness           → Outside claimed areas (if use-death-cause: false)
      ↓
5. Time-based           → Day or night settings
```

The first matching rule is applied. Higher rules override lower ones.

---

## Enable Advanced Mode

```yaml
advanced:
  enabled: true  # REQUIRED for any advanced feature
```

---

## Bypass Permission

```yaml
advanced:
  bypass-permission: true
```

Players with `dynamickeepinv.bypass` will **always** keep their inventory, ignoring all other rules.

**Use case:** Give this to staff or donators.

---

## Lands Integration

For servers using the [Lands](https://www.spigotmc.org/resources/lands.53313/) plugin:

```yaml
advanced:
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
        use-death-cause: false
        keep-items: false
        keep-xp: false
```

### Settings Explained

| Setting | Description |
|---------|-------------|
| `enabled` | Enable Lands integration |
| `override-lands` | `true` = override Lands' built-in keep inventory. `false` = let Lands handle it |
| `in-own-land` | Settings for land you own or are trusted in |
| `in-other-land` | Settings for someone else's land |
| `wilderness.enabled` | Enable wilderness rules |
| `wilderness.use-death-cause` | `true` = use PvP/PvE rules in wilderness. `false` = use wilderness settings |

### Recommended Setup

**Let Lands control its areas, plugin controls wilderness:**
```yaml
advanced:
  protection:
    lands:
      enabled: true
      override-lands: false  # Let Lands handle its areas
      wilderness:
        enabled: true
        use-death-cause: true  # Use PvP/PvE rules outside Lands
```

---

## GriefPrevention Integration

For servers using [GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/):

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
        use-death-cause: false
        keep-items: false
        keep-xp: false
```

Same logic as Lands - `in-own-claim` is claims you own or are trusted in.

---

## Death Cause Rules

Different rules based on how the player died:

```yaml
advanced:
  death-cause:
    enabled: true
    
    pvp:                    # Killed by another player
      keep-items: true
      keep-xp: true
    
    pve:                    # Everything else (mobs, fall, lava, etc.)
      keep-items: false
      keep-xp: true
```

### What counts as PvP?

- Direct player kill (sword, bow, etc.)
- Player's wolf/pet kills

### What counts as PvE?

- Mob kills (zombie, skeleton, creeper, etc.)
- Environmental (fall damage, lava, drowning, fire)
- Commands (`/kill`)

### Examples

| Scenario | Result |
|----------|--------|
| Player A kills Player B | PvP settings apply |
| Zombie kills player | PvE settings apply |
| Player falls into lava | PvE settings apply |
| Player dies in wilderness to mob | Wilderness or PvE (depending on config) |

---

## Economy System

Charge players to keep their inventory. Requires [Vault](https://www.spigotmc.org/resources/vault.34315/) + an economy plugin.

```yaml
advanced:
  economy:
    enabled: true
    cost: 100.0
    mode: "charge-to-keep"
```

### Modes

| Mode | When charged | Behavior if can't pay |
|------|--------------|----------------------|
| `charge-to-keep` | When player would keep items | Keeps items for free (no penalty) |
| `charge-to-bypass` | When player would lose items | Loses items (can't afford protection) |

**Example - charge-to-bypass:**
- Player dies at night (would lose items)
- Has $100? → Pay and keep items
- No money? → Lose items as normal

---

## Death Messages

Show players what happened to their inventory:

```yaml
advanced:
  death-message:
    enabled: true
    chat: true        # Show in chat
    action-bar: false # Show above hotbar
```

Messages show:
- Whether items/XP were kept or lost
- The reason (PvP, PvE, time-based, etc.)

---

## Time-based Item/XP Control

Separate control over items and XP:

```yaml
advanced:
  day:
    keep-items: true
    keep-xp: true
  
  night:
    keep-items: false
    keep-xp: true     # Keep XP but lose items at night
```

---

## Complete Example Configs

### PvP Server
No item loss in PvP, but PvE is punishing:
```yaml
advanced:
  enabled: true
  death-cause:
    enabled: true
    pvp:
      keep-items: true
      keep-xp: true
    pve:
      keep-items: false
      keep-xp: false
```

### Casual Survival with Lands
Keep items in claimed areas, lose them in wilderness:
```yaml
advanced:
  enabled: true
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
        use-death-cause: true  # Use PvP/PvE rules
  death-cause:
    enabled: true
    pvp:
      keep-items: true   # No penalty for PvP in wilderness
      keep-xp: true
    pve:
      keep-items: false  # Lose items to mobs in wilderness
      keep-xp: true
```

### Economy Server
Pay to keep items:
```yaml
advanced:
  enabled: true
  economy:
    enabled: true
    cost: 500.0
    mode: "charge-to-bypass"
  night:
    keep-items: false
    keep-xp: false
```

---

## Related Pages

- [Basic Configuration](Basic-Configuration) - Time and world settings
- [Permissions](Permissions) - Permission nodes
- [FAQ](FAQ) - Common questions
