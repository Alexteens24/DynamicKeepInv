package xyz.superez.dynamickeepinv;

import org.bukkit.Bukkit;
import xyz.superez.dynamickeepinv.hooks.AxGravesHook;
import xyz.superez.dynamickeepinv.hooks.GravesXHook;
import xyz.superez.dynamickeepinv.hooks.GriefPreventionHook;
import xyz.superez.dynamickeepinv.hooks.LandsHook;
import xyz.superez.dynamickeepinv.hooks.MMOItemsHook;
import xyz.superez.dynamickeepinv.hooks.TownyHook;
import xyz.superez.dynamickeepinv.hooks.WorldGuardHook;

public class IntegrationManager {

    private final DynamicKeepInvPlugin plugin;

    private LandsHook landsHook;
    private GriefPreventionHook griefPreventionHook;
    private WorldGuardHook worldGuardHook;
    private TownyHook townyHook;
    private GravesXHook gravesXHook;
    private AxGravesHook axGravesHook;
    private MMOItemsHook mmoItemsHook;

    public IntegrationManager(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        if (plugin.getConfig().getBoolean("integrations.lands.enabled", false)) {
            landsHook = new LandsHook(plugin);
        } else {
            landsHook = null;
        }

        if (plugin.getConfig().getBoolean("integrations.griefprevention.enabled", false)) {
            griefPreventionHook = new GriefPreventionHook(plugin);
        } else {
            griefPreventionHook = null;
        }

        if (plugin.getConfig().getBoolean("integrations.worldguard.enabled", false)) {
            worldGuardHook = new WorldGuardHook(plugin);
        } else {
            worldGuardHook = null;
        }

        if (plugin.getConfig().getBoolean("integrations.towny.enabled", false)) {
            townyHook = new TownyHook(plugin);
        } else {
            townyHook = null;
        }

        if (plugin.getConfig().getBoolean("integrations.gravesx.enabled", false)) {
            if (Bukkit.getPluginManager().getPlugin("GravesX") != null) {
                gravesXHook = new GravesXHook(plugin);
                if (!gravesXHook.setup()) {
                    gravesXHook = null;
                }
            } else {
                plugin.getLogger().warning("GravesX integration enabled in config, but GravesX plugin not found!");
                gravesXHook = null;
            }
        } else {
            gravesXHook = null;
        }

        if (plugin.getConfig().getBoolean("integrations.axgraves.enabled", false)) {
            if (Bukkit.getPluginManager().getPlugin("AxGraves") != null) {
                axGravesHook = new AxGravesHook(plugin);
                if (!axGravesHook.setup()) {
                    axGravesHook = null;
                }
            } else {
                plugin.getLogger().warning("AxGraves integration enabled in config, but AxGraves plugin not found!");
                axGravesHook = null;
            }
        } else {
            axGravesHook = null;
        }

        if (Bukkit.getPluginManager().getPlugin("MMOItems") != null) {
            mmoItemsHook = new MMOItemsHook(plugin);
            plugin.getLogger().info("MMOItems hooked!");
        } else {
            mmoItemsHook = null;
        }
    }

    public LandsHook getLandsHook() {
        return landsHook;
    }

    public boolean isLandsEnabled() {
        return landsHook != null && landsHook.isAvailable();
    }

    public GriefPreventionHook getGriefPreventionHook() {
        return griefPreventionHook;
    }

    public boolean isGriefPreventionEnabled() {
        return griefPreventionHook != null && griefPreventionHook.isAvailable();
    }

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardHook != null && worldGuardHook.isAvailable();
    }

    public TownyHook getTownyHook() {
        return townyHook;
    }

    public boolean isTownyEnabled() {
        return townyHook != null && townyHook.isAvailable();
    }

    public GravesXHook getGravesXHook() {
        return gravesXHook;
    }

    public boolean isGravesXEnabled() {
        return gravesXHook != null && gravesXHook.isEnabled();
    }

    public AxGravesHook getAxGravesHook() {
        return axGravesHook;
    }

    public boolean isAxGravesEnabled() {
        return axGravesHook != null && axGravesHook.isEnabled();
    }

    public MMOItemsHook getMMOItemsHook() {
        return mmoItemsHook;
    }

    public boolean isMMOItemsEnabled() {
        return mmoItemsHook != null;
    }
}
