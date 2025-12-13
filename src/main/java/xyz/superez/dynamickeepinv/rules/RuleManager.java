package xyz.superez.dynamickeepinv.rules;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

import java.util.ArrayList;
import java.util.List;

public class RuleManager {
    private final DynamicKeepInvPlugin plugin;
    private final List<DeathRule> rules = new ArrayList<>();

    public RuleManager(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerRule(DeathRule rule) {
        rules.add(rule);
    }

    public RuleResult evaluate(Player player, PlayerDeathEvent event) {
        for (DeathRule rule : rules) {
            RuleResult result = rule.evaluate(player, event);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
