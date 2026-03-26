package xyz.superez.dynamickeepinv.rules;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;
import xyz.superez.dynamickeepinv.StatsManager;

/**
 * Grants keep-inventory leniency on a player's very first death, if enabled.
 * config path: rules.first-death.*
 */
public class FirstDeathRule implements DeathRule {

    @Override
    public RuleResult evaluate(PlayerDeathEvent event, DynamicKeepInvPlugin plugin) {
        if (!plugin.getDKIConfig().firstDeathEnabled) return null;

        Player player = event.getEntity();
        StatsManager stats = plugin.getStatsManager();
        if (stats == null) return null;

        int totalDeaths = stats.getTotalDeaths(player.getUniqueId());
        if (totalDeaths == 0) {
            boolean keepItems = plugin.getDKIConfig().firstDeathKeepItems;
            boolean keepXp    = plugin.getDKIConfig().firstDeathKeepXp;
            plugin.debug("FirstDeathRule: first death for " + player.getName() + " — keepItems=" + keepItems);
            return new RuleResult(keepItems, keepXp, RuleReasons.FIRST_DEATH);
        }

        return null;
    }

    @Override
    public String getName() {
        return "FirstDeathRule";
    }
}
