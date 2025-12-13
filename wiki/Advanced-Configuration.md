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

## GravesX Integration

For servers using [GravesX](https://www.spigotmc.org/resources/gravesx.118271/), this plugin can automatically create graves when items are lost.

```yaml
advanced:
  gravesx:
    enabled: true
```

### How It Works

- If a player **keeps** their inventory (due to any rule above), no grave is created.
- If a player **loses** their inventory (due to night, PvP, etc.), a grave is created containing their items and XP.
- **Note:** If `keep-xp` is true but `keep-items` is false, the grave will contain items but 0 XP (since you kept the XP).

### Interaction with GUI Mode

If you use the [Death Confirmation GUI](#death-confirmation-gui) and GravesX:
1. Player dies -> GUI opens.
2. Player clicks **Pay** -> Keeps items (no grave).
3. Player clicks **Drop** (or timeout) -> Items are sent to a grave instead of dropping on the ground.

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
    gui:
      timeout: 30        # Seconds to decide (default: 30)
      expire-time: 300   # Store pending death if disconnected (default: 300 = 5 minutes)
```

### Modes

| Mode | When charged | Behavior if can't pay |
|------|--------------|----------------------|
| `charge-to-keep` | When player would keep items | Keeps items for free (no penalty) |
| `charge-to-bypass` | When player would lose items | Loses items (can't afford protection) |
| `gui` | Player chooses via GUI | Items dropped if timeout or player clicks Drop |

**Example - charge-to-bypass:**
- Player dies at night (would lose items)
- Has $100? → Pay and keep items
- No money? → Lose items as normal

---

## Death Confirmation GUI

The `gui` mode opens a confirmation GUI when a player dies, letting them choose whether to pay to keep their items or drop them.

```yaml
advanced:
  economy:
    enabled: true
    cost: 100.0
    mode: "gui"
    gui:
      timeout: 30        # Seconds to decide (default: 30)
      expire-time: 300   # Store pending death if disconnected (default: 300 = 5 minutes)
```

### How It Works

1. Player dies and respawns.
2. A 27-slot GUI appears with 3 options:
   - **Pay** (Green) - Pay the cost and keep items.
   - **Info** (Yellow) - Shows item count, cost, and time remaining.
   - **Drop** (Red) - Drop items at death location.
3. If player doesn't choose within `timeout` seconds, items are dropped (or put in a grave if GravesX is enabled).
4. If player disconnects, their pending death is saved for up to `expire-time` seconds.

### GUI Layout

```
┌─────────────────────────────────────────┐
│ [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]     │
│ [ ] [ ] [P] [ ] [I] [ ] [D] [ ] [ ]     │
│ [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]     │
└─────────────────────────────────────────┘
```

- **P**: Pay button (Green wool) - Click to pay and restore items.
- **I**: Info display (Yellow wool) - Hover to see details.
- **D**: Drop button (Red wool) - Click to drop items immediately.

### Player Commands

- `/dki confirm`: Reopens the GUI if it was closed or if the player wants to access it again (while pending death is still active).
- `/dki autopay`: Toggles auto-payment on death. If enabled, the player will automatically pay and keep items without seeing the GUI, provided they have enough funds.

### Technical Details & Edge Cases

- **Curse of Vanishing:** Items with this enchantment are explicitly destroyed and will **never** be dropped, restored, or put into a grave.
- **Exact Location:** The plugin stores the exact X, Y, Z coordinates of death. Drops will appear exactly where the player died.
- **Duplicate Prevention:** The plugin manually handles drops in GUI mode. It temporarily clears drops to prevent vanilla mechanics from duplicating items.
- **Folia Support:** On Folia servers, the plugin uses region schedulers to ensure drops and grave creation happen safely on the correct thread.
- **Asynchronous Data:** Database operations (saving/loading pending deaths) are performed asynchronously to prevent server lag.

| Scenario | Result |
|----------|--------|
| Player closes GUI | Warning message, GUI can be reopened with `/dki confirm`. |
| Player disconnects | Death saved, GUI shown on rejoin if within `expire-time`. |
| Timeout expires | Items dropped at death location automatically (or to grave). |
| Not enough money | Player sees insufficient funds message, can still click Drop. |
| Economy unavailable | Items dropped automatically (no GUI shown). |

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
