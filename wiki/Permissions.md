# Permissions

## Command Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `dynamickeepinv.admin` | Access to all commands | OP |
| `dynamickeepinv.status` | Use `/dki status` | OP |
| `dynamickeepinv.reload` | Use `/dki reload` | OP |
| `dynamickeepinv.toggle` | Use `/dki enable/disable/toggle` | OP |

## Special Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `dynamickeepinv.bypass` | Always keep inventory (requires `advanced.bypass-permission: true`) | false |

## Example: LuckPerms

```bash
# Give admin access
/lp group admin permission set dynamickeepinv.admin true

# Give VIP players bypass
/lp group vip permission set dynamickeepinv.bypass true
```
