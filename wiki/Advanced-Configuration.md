# Advanced Configuration

These settings give you fine-grained control over keep inventory behaviour.

---

## How It Works

When a player dies, the plugin evaluates rules in this order:

```
1. Bypass Permission     → Always keep (if player has dynamickeepinv.bypass)
      ↓
2. Protection (Lands / GriefPrevention)
             → in-own-land / in-other-land settings
      ↓
3. Death Cause           → PvP or PvE settings
      ↓
4. Time-based            → Day or night settings (always fires)
```

The **first** matching rule is applied. Rules that are disabled are skipped.

---

## Bypass Permission

```yaml
rules:
  bypass-permission: true
```

Players with `dynamickeepinv.bypass` will **always** keep inventory, ignoring all other rules.

**Use case:** Give this permission to staff or donators.

---

## Death Cause Rules

Override day/night defaults based on how the player died:

```yaml
rules:
  death-cause:
    enabled: true
    pvp:             # Killed by another player
      keep-items: true
      keep-xp: true
    pve:             # Everything else (mobs, environment)
      keep-items: false
      keep-xp: true
```

### What counts as PvP?

- Direct player kill (sword, bow, etc.)
- Player's tamed wolf killing another player

### What counts as PvE?

- Mob kills (zombie, skeleton, creeper, etc.)
- Environmental (fall, lava, drowning, fire)
- Commands (`/kill`)

---

## Lands Integration

For servers using the [Lands](https://www.spigotmc.org/resources/lands.53313/) plugin:

```yaml
integrations:
  lands:
    enabled: true
    override-lands: false

    in-own-land:
      keep-items: true
      keep-xp: true

    in-other-land:
      keep-items: false
      keep-xp: false

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
| `override-lands` | `true` = ignore Lands' own keepInventory flag. `false` = let Lands handle it |
| `in-own-land` | Settings when the player dies in land they own or are trusted in |
| `in-other-land` | Settings when the player dies in someone else's land |
| `wilderness.enabled` | Apply a rule when the player dies outside all Lands claims |
| `wilderness.use-death-cause` | `true` = fall through to DeathCause rules in wilderness |

### Recommended Setup

**Let Lands control its own areas; plugin controls wilderness:**
```yaml
integrations:
  lands:
    enabled: true
    override-lands: false
    wilderness:
      enabled: true
      use-death-cause: true  # Use PvP/PvE rules outside Lands
```

---

## GriefPrevention Integration

For servers using [GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/):

```yaml
integrations:
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

Same logic as Lands — `in-own-claim` applies to claims you own or are trusted in.

---

## GravesX / AxGraves Integration

When a player **loses** their inventory, the plugin can automatically create a grave at the death location rather than scattering items on the ground.

```yaml
integrations:
  gravesx:
    enabled: true   # enable GravesX
  axgraves:
    enabled: true   # enable AxGraves (fallback if GravesX didn't create a grave)
```

### How It Works

- If a player **keeps** their inventory, no grave is created.
- If a player **loses** their inventory, a grave is created containing their items.
- If `keep-xp` is `true` but `keep-items` is `false`, the grave contains items but 0 XP.

### Interaction with GUI Mode

1. Player dies → GUI opens.
2. Player clicks **Pay** → Keeps items (no grave).
3. Player clicks **Drop** or timeout expires → Items go to a grave instead of the ground.

---

## Economy System

Charge players to keep their inventory. Requires [Vault](https://www.spigotmc.org/resources/vault.34315/) and a compatible economy plugin.

```yaml
economy:
  enabled: true
  cost: 100.0
  # Modes: charge-to-keep | charge-to-bypass | gui
  mode: "charge-to-keep"
```

### Modes

| Mode | When charged | Behaviour if broke |
|------|--------------|-------------------|
| `charge-to-keep` | Player would keep items | Items kept anyway (no penalty) |
| `charge-to-bypass` | Player would lose items | Items lost (couldn't afford protection) |
| `gui` | Player chooses via death confirmation GUI | Items drop if timeout or player clicks Drop |

**Example — charge-to-bypass:**
- Player dies at night (would lose items)
- Has ≥ $100 → charged, items kept
- Has < $100 → items lost as normal

---

## Death Confirmation GUI (`gui` mode)

Opens a confirmation screen when a player dies, letting them choose whether to pay or drop items.

### How It Works

1. Player dies and respawns.
2. A GUI appears with three buttons:
   - **Pay** (green) — pay the cost and keep items.
   - **Info** (yellow) — hover to see item count, cost, and time remaining.
   - **Drop** (red) — drop items at death location immediately.
3. If the player doesn't choose within the timeout, items are dropped automatically.
4. If the player disconnects, their pending decision is held in the database until the server's configured expire window.

### Player Commands

- `/dki confirm` — Reopens the pending-death GUI.
- `/dki autopay` — Toggles automatic payment on death (skips the GUI).

### Edge Cases

| Scenario | Result |
|----------|--------|
| Player closes GUI | Warning shown; GUI can be reopened with `/dki confirm` |
| Player disconnects | Death saved in SQLite; GUI shown on rejoin |
| Timeout expires | Items dropped (or sent to grave if GravesX/AxGraves is enabled) |
| Not enough money | Insufficient-funds message shown; player can still click Drop |
| Economy unavailable | Items dropped automatically — no GUI shown |
| Curse of Vanishing | Item is destroyed; never dropped, restored, or put in a grave |

---

## Death Messages

Show players a message explaining what happened to their inventory:

```yaml
messages:
  death:
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
