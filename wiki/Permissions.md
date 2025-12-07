# Permissions

## Overview

| Permission | Description | Default |
|------------|-------------|----------|
| `dynamickeepinv.admin` | All admin command permissions | OP |
| `dynamickeepinv.bypass` | Always keep inventory | false |
| `dynamickeepinv.stats` | View own death statistics | true |
| `dynamickeepinv.stats.others` | View other players' statistics | OP |

---

## Permission Details

### `dynamickeepinv.admin`

Parent permission that includes all admin command permissions (status, reload, toggle). Give this to server administrators.

### `dynamickeepinv.bypass`

**Special permission** - Players with this permission **always keep their inventory** when they die, regardless of any other settings.

Requires `advanced.bypass-permission: true` in config (enabled by default).

**Use cases:**
- Staff members
- VIP/Donator ranks
- Event participants

### `dynamickeepinv.stats`

Allows players to view their own death statistics with `/dki stats`. Enabled by default for all players.

### `dynamickeepinv.stats.others`

Allows viewing other players' death statistics with `/dki stats <player>`. Only give to moderators or staff who need to check player stats.

---

## Permission Plugin Examples

### LuckPerms

```bash
# Admin access
/lp group admin permission set dynamickeepinv.admin true

# VIP bypass
/lp group vip permission set dynamickeepinv.bypass true

# Allow viewing other players' stats
/lp group mod permission set dynamickeepinv.stats.others true
```

### PermissionsEx (PEX)

```bash
/pex group admin add dynamickeepinv.admin
/pex group vip add dynamickeepinv.bypass
```

### GroupManager

```bash
/mangaddp admin dynamickeepinv.admin
/mangaddp vip dynamickeepinv.bypass
```

---

## Default Behavior

- **OPs**: Have all command permissions by default
- **Non-OPs**: Can only use `/dki` and `/dki help`
- **Bypass**: Nobody has bypass by default (must be explicitly granted)

---

## Related

- [Commands](Commands) - Command reference
- [Advanced Configuration](Advanced-Configuration) - Bypass permission settings
