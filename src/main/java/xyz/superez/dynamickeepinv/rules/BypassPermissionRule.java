package xyz.superez.dynamickeepinv.rules;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

public class BypassPermissionRule implements DeathRule {
    private final DynamicKeepInvPlugin plugin;

    public BypassPermissionRule(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public RuleResult evaluate(Player player, PlayerDeathEvent event) {
        if (plugin.getConfig().getBoolean("advanced.bypass-permission", true)) {
            if (player.hasPermission("dynamickeepinv.bypass")) {
                plugin.debug("Player " + player.getName() + " has bypass permission. Keeping inventory.");
                return new RuleResult(true, true, "bypass");
            }
        }
        return null;
    }
}
