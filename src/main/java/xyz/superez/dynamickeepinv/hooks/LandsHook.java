package xyz.superez.dynamickeepinv.hooks;

import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.land.Area;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

public class LandsHook {
    private final DynamicKeepInvPlugin plugin;
    private LandsIntegration landsApi;
    private boolean enabled = false;

    public LandsHook(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Lands") == null) {
            plugin.debug("Lands not found, hook disabled.");
            return;
        }
        try {
            landsApi = LandsIntegration.of(plugin);
            enabled = true;
            plugin.getLogger().info("Lands hook enabled!");
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.getLogger().warning("Lands API version incompatible. Hook disabled. Please update Lands or DynamicKeepInv.");
            plugin.debug("Lands API error: " + e.getMessage());
            enabled = false;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hook into Lands: " + e.getMessage());
            enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean isAvailable() {
        return enabled && landsApi != null;
    }

    public boolean isInLand(Location location) {
        if (!enabled || landsApi == null) return false;
        try {
            Area area = landsApi.getArea(location);
            return area != null;
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.getLogger().warning("Lands API changed. Disabling Lands hook.");
            enabled = false;
            return false;
        } catch (Exception e) {
            plugin.debug("Error checking Lands area: " + e.getMessage());
            return false;
        }
    }

    public boolean isInOwnLand(Player player) {
        if (!enabled || landsApi == null) return false;
        try {
            Area area = landsApi.getArea(player.getLocation());
            if (area == null) return false;
            return area.getLand().getOwnerUID().equals(player.getUniqueId()) ||
                   area.getLand().isTrusted(player.getUniqueId());
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.getLogger().warning("Lands API changed. Disabling Lands hook.");
            enabled = false;
            return false;
        } catch (Exception e) {
            plugin.debug("Error checking Lands ownership: " + e.getMessage());
            return false;
        }
    }

    public String getLandName(Location location) {
        if (!enabled || landsApi == null) return null;
        try {
            Area area = landsApi.getArea(location);
            if (area == null) return null;
            return area.getLand().getName();
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.getLogger().warning("Lands API changed. Disabling Lands hook.");
            enabled = false;
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
