package xyz.superez.dynamickeepinv.rules;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

public class DeathCauseRule implements DeathRule {
    private final DynamicKeepInvPlugin plugin;

    public DeathCauseRule(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public RuleResult evaluate(Player player, PlayerDeathEvent event) {
        if (plugin.getConfig().getBoolean("advanced.death-cause.enabled", false)) {
            boolean isPvp = player.getKiller() != null;
            String causePath = isPvp ? "advanced.death-cause.pvp" : "advanced.death-cause.pve";

            boolean keepItems = plugin.getConfig().getBoolean(causePath + ".keep-items", false);
            boolean keepXp = plugin.getConfig().getBoolean(causePath + ".keep-xp", false);
            String reason = isPvp ? "pvp" : "pve";

            return new RuleResult(keepItems, keepXp, reason);
        }
        return null;
    }
}
