package xyz.superez.dynamickeepinv.rules;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;
import xyz.superez.dynamickeepinv.hooks.GriefPreventionHook;
import xyz.superez.dynamickeepinv.hooks.LandsHook;

public class ProtectionRule implements DeathRule {

    @Override
    public RuleResult evaluate(PlayerDeathEvent event, DynamicKeepInvPlugin plugin) {
        Player player = event.getEntity();
        Location location = player.getLocation();

        // 1. Check Lands
        if (plugin.isLandsEnabled() && plugin.getConfig().getBoolean("integrations.lands.enabled", false)) {
            LandsHook lands = plugin.getLandsHook();
            boolean inLand = lands.isInLand(location);
            boolean overrideLands = plugin.getConfig().getBoolean("integrations.lands.override-lands", false);

            if (inLand) {
                if (!overrideLands) {
                    return new RuleResult(false, false, "lands-defer");
                }

                boolean isOwnLand = lands.isInOwnLand(player);
                String configPath = isOwnLand ? "integrations.lands.in-own-land" : "integrations.lands.in-other-land";

                if (plugin.getConfig().contains(configPath)) {
                    boolean keepItems = plugin.getConfig().getBoolean(configPath + ".keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean(configPath + ".keep-xp", false);
                    String reason = isOwnLand ? "lands-own" : "lands-other";
                    return new RuleResult(keepItems, keepXp, reason);
                }
            } else {
                // Wilderness
                if (plugin.getConfig().getBoolean("integrations.lands.wilderness.enabled", false)) {
                    boolean useDeathCause = plugin.getConfig().getBoolean("integrations.lands.wilderness.use-death-cause", false);
                    if (!useDeathCause) {
                         boolean keepItems = plugin.getConfig().getBoolean("integrations.lands.wilderness.keep-items", false);
                         boolean keepXp = plugin.getConfig().getBoolean("integrations.lands.wilderness.keep-xp", false);
                         return new RuleResult(keepItems, keepXp, "lands-wilderness");
                    }
                    // If useDeathCause is true, we return null to let DeathCauseRule handle it.
                }
            }
        }

        // 2. Check GriefPrevention
        if (plugin.isGriefPreventionEnabled() && plugin.getConfig().getBoolean("integrations.griefprevention.enabled", false)) {
            GriefPreventionHook gp = plugin.getGriefPreventionHook();
            if (gp.isInClaim(location)) {
                boolean isOwnClaim = gp.isInOwnClaim(player);
                String configPath = isOwnClaim ? "integrations.griefprevention.in-own-claim" : "integrations.griefprevention.in-other-claim";

                if (plugin.getConfig().contains(configPath)) {
                    boolean keepItems = plugin.getConfig().getBoolean(configPath + ".keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean(configPath + ".keep-xp", false);
                    String reason = isOwnClaim ? "gp-own" : "gp-other";
                    return new RuleResult(keepItems, keepXp, reason);
                }
            } else {
                 if (plugin.getConfig().getBoolean("integrations.griefprevention.wilderness.enabled", false)) {
                     boolean useDeathCause = plugin.getConfig().getBoolean("integrations.griefprevention.wilderness.use-death-cause", false);
                     if (!useDeathCause) {
                         boolean keepItems = plugin.getConfig().getBoolean("integrations.griefprevention.wilderness.keep-items", false);
                         boolean keepXp = plugin.getConfig().getBoolean("integrations.griefprevention.wilderness.keep-xp", false);
                         return new RuleResult(keepItems, keepXp, "gp-wilderness");
                     }
                 }
            }
        }

        return null;
    }

    @Override
    public String getName() {
        return "ProtectionRule";
    }
}
