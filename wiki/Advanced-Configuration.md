# Advanced Configuration

These settings give you fine-grained control over how and why players keep or lose inventory.

---

## How It Works

When a player dies, DynamicKeepInv evaluates rules in this order:

```text
1. Bypass Permission
2. First Death Rule
3. Death Streak Rule
4. Protection Integrations
5. Death Cause Rule
6. Time-based Fallback
```

The first matching rule wins.

---

## Bypass Permission

```yaml
rules:
  bypass-permission: true
```

Players with `dynamickeepinv.bypass` always keep inventory if this is enabled.

---

## Death Cause Rules

```yaml
rules:
  death-cause:
    enabled: true
    pvp:
      keep-items: true
      keep-xp: true
    pve:
      keep-items: false
      keep-xp: true
```

PvP includes direct player kills and player-owned wolves. PvE includes mobs, environment, and command kills.

---

## First-Death Rule

```yaml
rules:
  first-death:
    enabled: true
    keep-items: true
    keep-xp: true
```

This grants a safety net on a player's very first recorded death.

Note: this depends on the stats system. If stats are disabled or unavailable, this rule is skipped.

---

## Death Streak Rule

```yaml
rules:
  streak:
    enabled: true
    threshold: 3
    window-seconds: 300
    keep-items: false
    keep-xp: false
```

| Setting | Description |
|---------|-------------|
| `threshold` | Number of deaths required to trigger the rule |
| `window-seconds` | How long recent deaths stay in the streak window |
| `keep-items` | Whether items are kept after the threshold |
| `keep-xp` | Whether XP is kept after the threshold |

This tracking is in-memory and resets on server restart.

---

## Lands Integration

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

`override-lands: false` means Lands can keep controlling its own keepInventory behavior.

---

## GriefPrevention Integration

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

`in-own-claim` applies when the player owns or is trusted in the claim.

---

## WorldGuard Integration

```yaml
integrations:
  worldguard:
    enabled: true
    in-own-region:
      keep-items: true
      keep-xp: true
    in-other-region:
      keep-items: false
      keep-xp: false
    wilderness:
      enabled: false
      keep-items: false
      keep-xp: false
```

`in-own-region` applies when the player is owner/member of the region they died in.

---

## Towny Integration

```yaml
integrations:
  towny:
    enabled: true
    in-own-town:
      keep-items: true
      keep-xp: true
    in-other-town:
      keep-items: false
      keep-xp: false
    wilderness:
      enabled: false
      keep-items: false
      keep-xp: false
```

`in-own-town` applies when the player is a resident of the town they died in.

---

## GravesX / AxGraves Integration

```yaml
integrations:
  gravesx:
    enabled: true
  axgraves:
    enabled: true
  graves:
    fallback-on-fail: true
```

If a player loses items, the plugin attempts to create a grave. If grave creation fails, it logs a warning and falls back to normal item drop behavior.

---

## MMOItems Protected Tags

```yaml
hooks:
  mmoitems:
    protected-tags:
      - MMOITEMS_SOULBOUND
      - MY_CUSTOM_PROTECTED_TAG
```

Leave the list empty to use only the default `MMOITEMS_SOULBOUND` tag.

---

## Economy System

Requires Vault and a compatible economy plugin.

```yaml
economy:
  enabled: true
  cost: 100.0
  mode: "charge-to-keep"
```

### Modes

| Mode | When charged | If player cannot pay |
|------|--------------|----------------------|
| `charge-to-keep` | When the player would keep items | Items are still kept |
| `charge-to-bypass` | When the player would lose items | Items are lost normally |
| `gui` | When the player decides in the death GUI | Timeout or drop choice loses items |

---

## Death Confirmation GUI

`economy.mode: "gui"` opens a death confirmation GUI after death.

Player commands:

- `/dki confirm` reopens the pending GUI
- `/dki autopay` toggles automatic payment in GUI mode

Behavior summary:

- Pay: items kept
- Drop: items dropped or sent to a grave
- Timeout: items dropped automatically
- Disconnect: pending death stored until expiry

---

## Death Messages

```yaml
messages:
  death:
    enabled: true
    chat: true
    action-bar: false
```

These explain whether items/XP were kept or lost and why.

---

## Time-based Item/XP Control

```yaml
rules:
  day:
    keep-items: true
    keep-xp: true
  night:
    keep-items: false
    keep-xp: true
```

---

## Example Setups

### PvP Server

```yaml
rules:
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

```yaml
integrations:
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
      use-death-cause: true

rules:
  death-cause:
    enabled: true
    pvp:
      keep-items: true
      keep-xp: true
    pve:
      keep-items: false
      keep-xp: true
```

### Economy Server

```yaml
economy:
  enabled: true
  cost: 500.0
  mode: "charge-to-bypass"

rules:
  night:
    keep-items: false
    keep-xp: false
```

---

## Related Pages

- [Basic Configuration](Basic-Configuration)
- [Permissions](Permissions)
- [FAQ](FAQ)
