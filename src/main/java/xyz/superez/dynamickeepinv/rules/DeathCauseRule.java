package xyz.superez.dynamickeepinv.rules;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DKIConfig;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

public class DeathCauseRule implements DeathRule {

    @Override
    public RuleResult evaluate(PlayerDeathEvent event, DynamicKeepInvPlugin plugin) {
        DKIConfig cfg = plugin.getDKIConfig();
        if (!cfg.deathCauseEnabled) {
            return null;
        }

        Player player = event.getEntity();
        boolean isPvp = player.getKiller() != null;

        boolean keepItems = isPvp ? cfg.pvpKeepItems : cfg.pveKeepItems;
        boolean keepXp = isPvp ? cfg.pvpKeepXp : cfg.pveKeepXp;
        String reason = isPvp ? RuleReasons.PVP : RuleReasons.PVE;

        return new RuleResult(keepItems, keepXp, reason);
    }

    @Override
    public String getName() {
        return "DeathCauseRule";
    }
}
