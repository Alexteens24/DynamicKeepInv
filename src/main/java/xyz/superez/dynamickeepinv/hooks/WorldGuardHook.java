package xyz.superez.dynamickeepinv.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

public class WorldGuardHook {
    private final DynamicKeepInvPlugin plugin;
    private volatile boolean enabled = false;

    public WorldGuardHook(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            plugin.debug("WorldGuard not found, hook disabled.");
            return;
        }
        try {
            // Verify WorldGuard API is accessible
            WorldGuard.getInstance();
            enabled = true;
            plugin.getLogger().info("WorldGuard hook enabled!");
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.getLogger().warning("WorldGuard API version incompatible. Hook disabled. Please update WorldGuard or DynamicKeepInv.");
            plugin.debug("WorldGuard API error: " + e.getMessage());
            enabled = false;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hook into WorldGuard: " + e.getMessage());
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
     * Returns true if the given location is inside at least one non-global WorldGuard region.
     */
    public boolean isInRegion(Location location) {
        if (!enabled) return false;
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(location));
            return regions.getRegions().stream().anyMatch(r -> !r.getId().equalsIgnoreCase("__global__"));
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.getLogger().warning("WorldGuard API changed. Disabling WG hook.");
            enabled = false;
            return false;
        } catch (Exception e) {
            plugin.debug("Error checking WorldGuard region: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns true if the player is a member/owner of ALL non-global regions at their death location.
     */
    public boolean isInOwnRegion(Player player) {
        if (!enabled) return false;
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
            LocalPlayer lp = WorldGuardPlugin.inst().wrapPlayer(player);
            return regions.isMemberOfAll(lp);
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.getLogger().warning("WorldGuard API changed. Disabling WG hook.");
            enabled = false;
            return false;
        } catch (Exception e) {
            plugin.debug("Error checking WorldGuard region membership: " + e.getMessage());
            return false;
        }
    }
}
