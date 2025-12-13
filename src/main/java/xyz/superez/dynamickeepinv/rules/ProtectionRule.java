package xyz.superez.dynamickeepinv.rules;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;
import xyz.superez.dynamickeepinv.hooks.GriefPreventionHook;
import xyz.superez.dynamickeepinv.hooks.LandsHook;

public class ProtectionRule implements DeathRule {
    private final DynamicKeepInvPlugin plugin;

    public ProtectionRule(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public RuleResult evaluate(Player player, PlayerDeathEvent event) {
        Location deathLocation = player.getLocation();
        plugin.debug("Checking protection plugins...");

        // Lands Check
        if (plugin.isLandsEnabled() && plugin.getConfig().getBoolean("advanced.protection.lands.enabled", false)) {
            LandsHook lands = plugin.getLandsHook();
            boolean inLand = lands.isInLand(deathLocation);
            boolean overrideLands = plugin.getConfig().getBoolean("advanced.protection.lands.override-lands", false);

            if (inLand) {
                if (!overrideLands) {
                    plugin.debug("In land but override-lands=false, letting Lands handle it.");
                    return new RuleResult(false, false, "lands-defer");
                }

                boolean isOwnLand = lands.isInOwnLand(player);
                String configPath = isOwnLand ? "advanced.protection.lands.in-own-land" : "advanced.protection.lands.in-other-land";

                if (plugin.getConfig().contains(configPath)) {
                    boolean keepItems = plugin.getConfig().getBoolean(configPath + ".keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean(configPath + ".keep-xp", false);
                    String reason = isOwnLand ? "lands-own" : "lands-other";
                    return new RuleResult(keepItems, keepXp, reason);
                }
            } else {
                // Wilderness
                if (plugin.getConfig().getBoolean("advanced.protection.lands.wilderness.enabled", false)) {
                    boolean useDeathCause = plugin.getConfig().getBoolean("advanced.protection.lands.wilderness.use-death-cause", false);
                    if (useDeathCause) {
                        return null; // Fall through to Death Cause
                    }

                    boolean keepItems = plugin.getConfig().getBoolean("advanced.protection.lands.wilderness.keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean("advanced.protection.lands.wilderness.keep-xp", false);
                    return new RuleResult(keepItems, keepXp, "lands-wilderness");
                }
            }
        }

        // GriefPrevention Check
        if (plugin.isGriefPreventionEnabled() && plugin.getConfig().getBoolean("advanced.protection.griefprevention.enabled", false)) {
            GriefPreventionHook gp = plugin.getGriefPreventionHook();
            if (gp.isInClaim(deathLocation)) {
                boolean isOwnClaim = gp.isInOwnClaim(player);
                String configPath = isOwnClaim ? "advanced.protection.griefprevention.in-own-claim" : "advanced.protection.griefprevention.in-other-claim";

                if (plugin.getConfig().contains(configPath)) {
                    boolean keepItems = plugin.getConfig().getBoolean(configPath + ".keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean(configPath + ".keep-xp", false);
                    String reason = isOwnClaim ? "gp-own" : "gp-other";
                    return new RuleResult(keepItems, keepXp, reason);
                }
            } else {
                // Wilderness
                if (plugin.getConfig().getBoolean("advanced.protection.griefprevention.wilderness.enabled", false)) {
                    boolean useDeathCause = plugin.getConfig().getBoolean("advanced.protection.griefprevention.wilderness.use-death-cause", false);
                    if (useDeathCause) {
                        return null; // Fall through to Death Cause
                    }

                    boolean keepItems = plugin.getConfig().getBoolean("advanced.protection.griefprevention.wilderness.keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean("advanced.protection.griefprevention.wilderness.keep-xp", false);
                    return new RuleResult(keepItems, keepXp, "gp-wilderness");
                }
            }
        }

        return null;
    }
}
