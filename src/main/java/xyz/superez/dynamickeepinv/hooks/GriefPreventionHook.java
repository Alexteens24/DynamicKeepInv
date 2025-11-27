package xyz.superez.dynamickeepinv.hooks;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.DataStore;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

public class GriefPreventionHook {
    private final DynamicKeepInvPlugin plugin;
    private GriefPrevention gp;
    private boolean enabled = false;

    public GriefPreventionHook(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (plugin.getServer().getPluginManager().getPlugin("GriefPrevention") == null) {
            plugin.debug("GriefPrevention not found, hook disabled.");
            return;
        }
        try {
            gp = GriefPrevention.instance;
            if (gp != null) {
                enabled = true;
                plugin.getLogger().info("GriefPrevention hook enabled!");
            }
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.getLogger().warning("GriefPrevention API version incompatible. Hook disabled. Please update GriefPrevention or DynamicKeepInv.");
            plugin.debug("GriefPrevention API error: " + e.getMessage());
            enabled = false;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hook into GriefPrevention: " + e.getMessage());
            enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean isAvailable() {
        return enabled && gp != null;
    }

    public boolean isInClaim(Location location) {
        if (!enabled || gp == null) return false;
        try {
            DataStore dataStore = gp.dataStore;
            Claim claim = dataStore.getClaimAt(location, false, null);
            return claim != null;
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.getLogger().warning("GriefPrevention API changed. Disabling GP hook.");
            enabled = false;
            return false;
        } catch (Exception e) {
            plugin.debug("Error checking GP claim: " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    public boolean isInOwnClaim(Player player) {
        if (!enabled || gp == null) return false;
        try {
            DataStore dataStore = gp.dataStore;
            Claim claim = dataStore.getClaimAt(player.getLocation(), false, null);
            if (claim == null) return false;
            return claim.getOwnerID().equals(player.getUniqueId()) ||
                   claim.allowAccess(player) == null;
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.getLogger().warning("GriefPrevention API changed. Disabling GP hook.");
            enabled = false;
            return false;
        } catch (Exception e) {
            plugin.debug("Error checking GP ownership: " + e.getMessage());
            return false;
        }
    }

    public String getClaimOwnerName(Location location) {
        if (!enabled || gp == null) return null;
        try {
            DataStore dataStore = gp.dataStore;
            Claim claim = dataStore.getClaimAt(location, false, null);
            if (claim == null) return null;
            return claim.getOwnerName();
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.getLogger().warning("GriefPrevention API changed. Disabling GP hook.");
            enabled = false;
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
