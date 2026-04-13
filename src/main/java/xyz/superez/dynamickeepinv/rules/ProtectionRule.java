package xyz.superez.dynamickeepinv.rules;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.superez.dynamickeepinv.DKIConfig;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;
import xyz.superez.dynamickeepinv.hooks.GriefPreventionHook;
import xyz.superez.dynamickeepinv.hooks.LandsHook;
import xyz.superez.dynamickeepinv.hooks.TownyHook;
import xyz.superez.dynamickeepinv.hooks.WorldGuardHook;

public class ProtectionRule implements DeathRule {

    @Override
    public RuleResult evaluate(PlayerDeathEvent event, DynamicKeepInvPlugin plugin) {
        DKIConfig cfg = plugin.getDKIConfig();
        Player player = event.getEntity();
        Location location = player.getLocation();

        // 1. Check Lands
        if (plugin.isLandsEnabled() && cfg.landsEnabled) {
            LandsHook lands = plugin.getLandsHook();
            boolean inLand = lands.isInLand(location);
            boolean overrideLands = cfg.landsOverride;

            if (inLand) {
                if (!overrideLands) {
                    return new RuleResult(false, false, RuleReasons.LANDS_DEFER);
                }

                boolean isOwnLand = lands.isInOwnLand(player);
                boolean keepItems = isOwnLand ? cfg.landsOwnKeepItems : cfg.landsOtherKeepItems;
                boolean keepXp = isOwnLand ? cfg.landsOwnKeepXp : cfg.landsOtherKeepXp;
                String reason = isOwnLand ? RuleReasons.LANDS_OWN : RuleReasons.LANDS_OTHER;
                return new RuleResult(keepItems, keepXp, reason);
            } else {
                // Wilderness
                if (cfg.landsWildernessEnabled) {
                    boolean useDeathCause = cfg.landsWildernessUseDeathCause;
                    if (!useDeathCause) {
                         boolean keepItems = cfg.landsWildernessKeepItems;
                         boolean keepXp = cfg.landsWildernessKeepXp;
                         return new RuleResult(keepItems, keepXp, RuleReasons.LANDS_WILDERNESS);
                    }
                    // If useDeathCause is true, we return null to let DeathCauseRule handle it.
                }
            }
        }

        // 2. Check GriefPrevention
        if (plugin.isGriefPreventionEnabled() && cfg.gpEnabled) {
            GriefPreventionHook gp = plugin.getGriefPreventionHook();
            if (gp.isInClaim(location)) {
                boolean isOwnClaim = gp.isInOwnClaim(player);
                boolean keepItems = isOwnClaim ? cfg.gpOwnKeepItems : cfg.gpOtherKeepItems;
                boolean keepXp = isOwnClaim ? cfg.gpOwnKeepXp : cfg.gpOtherKeepXp;
                String reason = isOwnClaim ? RuleReasons.GP_OWN : RuleReasons.GP_OTHER;
                return new RuleResult(keepItems, keepXp, reason);
            } else {
                 if (cfg.gpWildernessEnabled) {
                     boolean useDeathCause = cfg.gpWildernessUseDeathCause;
                     if (!useDeathCause) {
                         boolean keepItems = cfg.gpWildernessKeepItems;
                         boolean keepXp = cfg.gpWildernessKeepXp;
                         return new RuleResult(keepItems, keepXp, RuleReasons.GP_WILDERNESS);
                     }
                 }
            }
        }

        // 3. Check WorldGuard
        if (plugin.isWorldGuardEnabled() && cfg.worldGuardEnabled) {
            WorldGuardHook wg = plugin.getWorldGuardHook();
            if (wg.isInRegion(location)) {
                boolean isOwnRegion = wg.isInOwnRegion(player);
                boolean keepItems = isOwnRegion ? cfg.worldGuardOwnRegionKeepItems : cfg.worldGuardOtherRegionKeepItems;
                boolean keepXp = isOwnRegion ? cfg.worldGuardOwnRegionKeepXp : cfg.worldGuardOtherRegionKeepXp;
                String reason = isOwnRegion ? RuleReasons.WG_OWN : RuleReasons.WG_OTHER;
                return new RuleResult(keepItems, keepXp, reason);
            } else {
                if (cfg.worldGuardWildernessEnabled) {
                    boolean keepItems = cfg.worldGuardWildernessKeepItems;
                    boolean keepXp = cfg.worldGuardWildernessKeepXp;
                    return new RuleResult(keepItems, keepXp, RuleReasons.WG_WILDERNESS);
                }
            }
        }

        // 4. Check Towny
        if (plugin.isTownyEnabled() && cfg.townyEnabled) {
            TownyHook towny = plugin.getTownyHook();
            if (towny.isInTown(location)) {
                boolean isResident = towny.isInOwnTown(player);
                boolean keepItems = isResident ? cfg.townyOwnTownKeepItems : cfg.townyOtherTownKeepItems;
                boolean keepXp = isResident ? cfg.townyOwnTownKeepXp : cfg.townyOtherTownKeepXp;
                String reason = isResident ? RuleReasons.TOWNY_OWN : RuleReasons.TOWNY_OTHER;
                return new RuleResult(keepItems, keepXp, reason);
            } else {
                if (cfg.townyWildernessEnabled) {
                    boolean keepItems = cfg.townyWildernessKeepItems;
                    boolean keepXp = cfg.townyWildernessKeepXp;
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
