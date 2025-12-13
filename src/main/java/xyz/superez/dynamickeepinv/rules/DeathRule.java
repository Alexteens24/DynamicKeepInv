package xyz.superez.dynamickeepinv.rules;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

public interface DeathRule {
    RuleResult evaluate(Player player, PlayerDeathEvent event);
}
