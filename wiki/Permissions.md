# Permissions

Permission nodes for DynamicKeepInv.

## Admin Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `dynamickeepinv.admin` | `op` | Full access to admin commands (`reload`, `status`, `enable`, `disable`, `toggle`, `test`). |

## User Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `dynamickeepinv.use` | `true` | Allows using basic commands like `/dki confirm` and `/dki autopay`. |
| `dynamickeepinv.bypass` | `false` | Bypasses all inventory loss rules. Player will always keep inventory. |
| `dynamickeepinv.stats` | `true` | Allows viewing own stats with `/dki stats`. |
| `dynamickeepinv.stats.others` | `op` | Allows viewing other players' stats with `/dki stats <player>`. |

## Recommended Setup

- **Default players:** Should have `dynamickeepinv.use` and `dynamickeepinv.stats`.
- **Admins:** Should have `dynamickeepinv.admin` and `dynamickeepinv.stats.others`.
- **VIP/Donors:** Can be given `dynamickeepinv.bypass` to never lose items.

## Practical Notes

- `dynamickeepinv.bypass` changes gameplay behavior, not command access.
- `dynamickeepinv.admin` is required for `/dki test [player]`.
