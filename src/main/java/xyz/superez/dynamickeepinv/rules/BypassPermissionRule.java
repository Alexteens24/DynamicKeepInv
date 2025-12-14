package xyz.superez.dynamickeepinv.rules;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

public class BypassPermissionRule implements DeathRule {

    @Override
    public RuleResult evaluate(PlayerDeathEvent event, DynamicKeepInvPlugin plugin) {
        if (!plugin.getConfig().getBoolean("rules.bypass-permission", true)) {
            return null;
        }

        Player player = event.getEntity();
        if (player.hasPermission("dynamickeepinv.bypass")) {
            return new RuleResult(true, true, "bypass");
        }

        return null;
    }

    @Override
    public String getName() {
        return "BypassPermissionRule";
    }
}
