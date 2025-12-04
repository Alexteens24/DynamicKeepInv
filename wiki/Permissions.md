# Permissions

## Overview

| Permission | Description | Default |
|------------|-------------|----------|
| `dynamickeepinv.admin` | All permissions below | OP |
| `dynamickeepinv.status` | Use `/dki status` | OP |
| `dynamickeepinv.reload` | Use `/dki reload` | OP |
| `dynamickeepinv.toggle` | Use `/dki enable/disable/toggle` | OP |
| `dynamickeepinv.bypass` | Always keep inventory | false |

---

## Permission Details

### `dynamickeepinv.admin`

Parent permission that includes all command permissions. Give this to server administrators.

### `dynamickeepinv.status`

Allows viewing plugin status with `/dki status`. Safe to give to moderators.

### `dynamickeepinv.reload`

Allows reloading config files. Only give to trusted staff.

### `dynamickeepinv.toggle`

Allows enabling/disabling the plugin. Only give to trusted staff.

### `dynamickeepinv.bypass`

**Special permission** - Players with this permission **always keep their inventory** when they die, regardless of any other settings.

Requires `advanced.bypass-permission: true` in config (enabled by default).

**Use cases:**
- Staff members
- VIP/Donator ranks
- Event participants

---

## Permission Plugin Examples

### LuckPerms

```bash
# Admin access
/lp group admin permission set dynamickeepinv.admin true

# VIP bypass
/lp group vip permission set dynamickeepinv.bypass true

# Mod can view status
/lp group mod permission set dynamickeepinv.status true
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
