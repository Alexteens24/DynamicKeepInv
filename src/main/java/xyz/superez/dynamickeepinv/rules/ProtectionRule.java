package xyz.superez.dynamickeepinv.rules;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;
import xyz.superez.dynamickeepinv.hooks.GriefPreventionHook;
import xyz.superez.dynamickeepinv.hooks.LandsHook;
import xyz.superez.dynamickeepinv.hooks.TownyHook;
import xyz.superez.dynamickeepinv.hooks.WorldGuardHook;

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
                    return new RuleResult(false, false, RuleReasons.LANDS_DEFER);
                }

                boolean isOwnLand = lands.isInOwnLand(player);
                String configPath = isOwnLand ? "integrations.lands.in-own-land" : "integrations.lands.in-other-land";

                if (plugin.getConfig().contains(configPath)) {
                    boolean keepItems = plugin.getConfig().getBoolean(configPath + ".keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean(configPath + ".keep-xp", false);
                    String reason = isOwnLand ? RuleReasons.LANDS_OWN : RuleReasons.LANDS_OTHER;
                    return new RuleResult(keepItems, keepXp, reason);
                }
            } else {
                // Wilderness
                if (plugin.getConfig().getBoolean("integrations.lands.wilderness.enabled", false)) {
                    boolean useDeathCause = plugin.getConfig().getBoolean("integrations.lands.wilderness.use-death-cause", false);
                    if (!useDeathCause) {
                         boolean keepItems = plugin.getConfig().getBoolean("integrations.lands.wilderness.keep-items", false);
                         boolean keepXp = plugin.getConfig().getBoolean("integrations.lands.wilderness.keep-xp", false);
                         return new RuleResult(keepItems, keepXp, RuleReasons.LANDS_WILDERNESS);
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
                    String reason = isOwnClaim ? RuleReasons.GP_OWN : RuleReasons.GP_OTHER;
                    return new RuleResult(keepItems, keepXp, reason);
                }
            } else {
                 if (plugin.getConfig().getBoolean("integrations.griefprevention.wilderness.enabled", false)) {
                     boolean useDeathCause = plugin.getConfig().getBoolean("integrations.griefprevention.wilderness.use-death-cause", false);
                     if (!useDeathCause) {
                         boolean keepItems = plugin.getConfig().getBoolean("integrations.griefprevention.wilderness.keep-items", false);
                         boolean keepXp = plugin.getConfig().getBoolean("integrations.griefprevention.wilderness.keep-xp", false);
                         return new RuleResult(keepItems, keepXp, RuleReasons.GP_WILDERNESS);
                     }
                 }
            }
        }

        // 3. Check WorldGuard
        if (plugin.isWorldGuardEnabled() && plugin.getConfig().getBoolean("integrations.worldguard.enabled", false)) {
            WorldGuardHook wg = plugin.getWorldGuardHook();
            if (wg.isInRegion(location)) {
                boolean isOwnRegion = wg.isInOwnRegion(player);
                String configPath = isOwnRegion ? "integrations.worldguard.in-own-region" : "integrations.worldguard.in-other-region";
                boolean keepItems = plugin.getConfig().getBoolean(configPath + ".keep-items", isOwnRegion);
                boolean keepXp    = plugin.getConfig().getBoolean(configPath + ".keep-xp",    isOwnRegion);
                String reason = isOwnRegion ? RuleReasons.WG_OWN : RuleReasons.WG_OTHER;
                return new RuleResult(keepItems, keepXp, reason);
            } else {
                if (plugin.getConfig().getBoolean("integrations.worldguard.wilderness.enabled", false)) {
                    boolean keepItems = plugin.getConfig().getBoolean("integrations.worldguard.wilderness.keep-items", false);
                    boolean keepXp    = plugin.getConfig().getBoolean("integrations.worldguard.wilderness.keep-xp",    false);
                    return new RuleResult(keepItems, keepXp, RuleReasons.WG_WILDERNESS);
                }
            }
        }

        // 4. Check Towny
        if (plugin.isTownyEnabled() && plugin.getConfig().getBoolean("integrations.towny.enabled", false)) {
            TownyHook towny = plugin.getTownyHook();
            if (towny.isInTown(location)) {
                boolean isResident = towny.isInOwnTown(player);
                String configPath = isResident ? "integrations.towny.in-own-town" : "integrations.towny.in-other-town";
                boolean keepItems = plugin.getConfig().getBoolean(configPath + ".keep-items", isResident);
                boolean keepXp    = plugin.getConfig().getBoolean(configPath + ".keep-xp",    isResident);
                String reason = isResident ? RuleReasons.TOWNY_OWN : RuleReasons.TOWNY_OTHER;
                return new RuleResult(keepItems, keepXp, reason);
            } else {
                if (plugin.getConfig().getBoolean("integrations.towny.wilderness.enabled", false)) {
                    boolean keepItems = plugin.getConfig().getBoolean("integrations.towny.wilderness.keep-items", false);
                    boolean keepXp    = plugin.getConfig().getBoolean("integrations.towny.wilderness.keep-xp",    false);
                    return new RuleResult(keepItems, keepXp, RuleReasons.TOWNY_WILDERNESS);
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
