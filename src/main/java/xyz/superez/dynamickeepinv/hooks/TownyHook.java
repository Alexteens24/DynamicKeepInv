package xyz.superez.dynamickeepinv.hooks;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

public class TownyHook {
    private final DynamicKeepInvPlugin plugin;
    private volatile boolean enabled = false;

    public TownyHook(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Towny") == null) {
            plugin.debug("Towny not found, hook disabled.");
            return;
        }
        try {
            // Verify Towny API is accessible
            TownyAPI.getInstance();
            enabled = true;
            plugin.getLogger().info("Towny hook enabled!");
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.getLogger().warning("Towny API version incompatible. Hook disabled. Please update Towny or DynamicKeepInv.");
            plugin.debug("Towny API error: " + e.getMessage());
            enabled = false;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hook into Towny: " + e.getMessage());
            enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAvailable() {
        return enabled;
    }

    /**
     * Returns true if the given location is claimed by a Towny town.
     */
    public boolean isInTown(Location location) {
        if (!enabled) return false;
        try {
            TownBlock tb = TownyAPI.getInstance().getTownBlock(location);
            return tb != null && tb.hasTown();
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.getLogger().warning("Towny API changed. Disabling Towny hook.");
            enabled = false;
            return false;
        } catch (Exception e) {
            plugin.debug("Error checking Towny town block: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns true if the player is a resident of the town at their death location.
     */
    public boolean isInOwnTown(Player player) {
        if (!enabled) return false;
        try {
            TownBlock tb = TownyAPI.getInstance().getTownBlock(player.getLocation());
            if (tb == null || !tb.hasTown()) return false;
            Town town = tb.getTown();
            Resident resident = TownyAPI.getInstance().getResident(player.getUniqueId());
            return resident != null && town.hasResident(resident);
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.getLogger().warning("Towny API changed. Disabling Towny hook.");
            enabled = false;
            return false;
        } catch (Exception e) {
            plugin.debug("Error checking Towny residency: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns the name of the town at the given location, or null if not in a town.
     */
    public String getTownName(Location location) {
        if (!enabled) return null;
        try {
            TownBlock tb = TownyAPI.getInstance().getTownBlock(location);
            if (tb == null || !tb.hasTown()) return null;
            return tb.getTown().getName();
        } catch (Exception e) {
            plugin.debug("Error getting Towny town name: " + e.getMessage());
            return null;
        }
    }
}
