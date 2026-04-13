package xyz.superez.dynamickeepinv.rules;

import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class RuleManager {
    private final DynamicKeepInvPlugin plugin;
    private final List<DeathRule> rules = new ArrayList<>();

    public RuleManager(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerRule(DeathRule rule) {
        rules.add(rule);
        plugin.debug("Registered death rule: " + rule.getName());
    }

    public void clearRules() {
        rules.clear();
    }

    public List<String> getRuleNames() {
        List<String> names = new ArrayList<>();
        for (DeathRule rule : rules) {
            names.add(rule.getName());
        }
        return names;
    }

    public RuleResult evaluate(PlayerDeathEvent event) {
        for (DeathRule rule : rules) {
            plugin.debug("Evaluating rule: " + rule.getName());
            try {
                RuleResult result = rule.evaluate(event, plugin);
                if (result != null) {
                    plugin.debug("Rule " + rule.getName() + " made a decision: keepItems=" + result.keepItems() + ", keepXp=" + result.keepXp() + ", reason=" + result.reason());
                    return result;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error evaluating rule " + rule.getName(), e);
            }
        }
        return null;
    }
}
