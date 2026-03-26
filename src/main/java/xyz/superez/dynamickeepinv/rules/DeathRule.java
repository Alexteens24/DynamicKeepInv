package xyz.superez.dynamickeepinv.rules;

import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

public interface DeathRule {
    /**
     * Evaluate the death rule.
     * @param event The death event.
     * @param plugin The plugin instance.
     * @return A RuleResult if this rule makes a decision, or null if it defers to the next rule.
     */
    RuleResult evaluate(PlayerDeathEvent event, DynamicKeepInvPlugin plugin);

    /**
     * Get the name of this rule for debugging purposes.
     * @return The rule name.
     */
    String getName();
}
